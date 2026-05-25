package com.stylish.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.stylish.auth.api.DmitriyConflictException;
import com.stylish.auth.api.DmitriyUnauthorizedException;
import com.stylish.auth.config.DmitriyJwtProperties;
import com.stylish.auth.dto.DmitriyTokenPairResponse;
import com.stylish.auth.dto.DmitriyUserResponse;
import com.stylish.auth.event.DmitriyAuthEventService;
import com.stylish.auth.event.DmitriyAuthEventType;
import com.stylish.auth.token.DmitriyRefreshTokenEntity;
import com.stylish.auth.token.DmitriyRefreshTokenRepository;
import com.stylish.auth.user.DmitriyUserEntity;
import com.stylish.auth.user.DmitriyUserLookupService;
import com.stylish.auth.user.DmitriyUserRepository;
import com.stylish.auth.user.DmitriyUserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DmitriyAuthServiceTest {

	@Mock DmitriyUserRepository userRepository;
	@Mock DmitriyRefreshTokenRepository refreshTokenRepository;
	@Mock PasswordEncoder passwordEncoder;
	@Mock DmitriyTokenService tokenService;
	@Mock DmitriyJwtProperties jwtProperties;
	@Mock DmitriyAuthEventService authEventService;
	@Mock DmitriyUserLookupService userLookupService;

	@InjectMocks DmitriyAuthService authService;

	private final Instant now = Instant.parse("2025-01-01T00:00:00Z");

	private DmitriyUserEntity activeUser(String email) {
		DmitriyUserEntity u = new DmitriyUserEntity(email, "hash");
		ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(u, "createdAt", now);
		ReflectionTestUtils.setField(u, "status", DmitriyUserStatus.ACTIVE);
		return u;
	}

	@BeforeEach
	void setupTokenService() {
		when(jwtProperties.refreshTtl()).thenReturn(Duration.ofDays(30));
		when(tokenService.newRefreshTokenValue()).thenReturn(
				new DmitriyTokenService.RefreshTokenValue(UUID.randomUUID(), "secret-abc")
		);
		when(tokenService.createAccessToken(any(), any())).thenReturn("access.jwt.token");
		when(tokenService.accessExpiresInSeconds(any())).thenReturn(900L);
		when(passwordEncoder.encode(anyString())).thenReturn("hashed");
	}

	@Test
	void register_happyPath_savesUserAndEmitsEvent() {
		when(userRepository.existsByEmailIgnoreCase("alice@x.io")).thenReturn(false);
		DmitriyUserEntity saved = activeUser("alice@x.io");
		when(userRepository.save(any(DmitriyUserEntity.class))).thenReturn(saved);

		DmitriyUserResponse resp = authService.register("Alice@X.io", "pw", "1.1.1.1", "ua");

		assertThat(resp.email()).isEqualTo("alice@x.io");
		verify(authEventService).record(eq(saved.getId()), eq("alice@x.io"), eq(DmitriyAuthEventType.REGISTER), eq("1.1.1.1"), eq("ua"));
	}

	@Test
	void register_duplicateEmail_throwsConflict() {
		when(userRepository.existsByEmailIgnoreCase("dup@x.io")).thenReturn(true);

		assertThatThrownBy(() -> authService.register("dup@x.io", "pw", null, null))
				.isInstanceOf(DmitriyConflictException.class);

		verify(userRepository, never()).save(any());
		verify(authEventService, never()).record(any(), any(), any(), any(), any());
	}

	@Test
	void login_unknownEmail_throwsAndRecordsFailed() {
		when(userLookupService.findByEmailOrNull("ghost@x.io")).thenReturn(null);

		assertThatThrownBy(() -> authService.login("ghost@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(DmitriyUnauthorizedException.class);

		verify(authEventService).record(eq(null), eq("ghost@x.io"), eq(DmitriyAuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_wrongPassword_throwsAndRecordsFailed() {
		DmitriyUserEntity user = activeUser("bob@x.io");
		when(userLookupService.findByEmailOrNull("bob@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login("bob@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(DmitriyUnauthorizedException.class);

		verify(authEventService).record(eq(user.getId()), eq("bob@x.io"), eq(DmitriyAuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_disabled_throwsAndRecordsFailed() {
		DmitriyUserEntity user = activeUser("carol@x.io");
		ReflectionTestUtils.setField(user, "status", DmitriyUserStatus.DISABLED);
		when(userLookupService.findByEmailOrNull("carol@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

		assertThatThrownBy(() -> authService.login("carol@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(DmitriyUnauthorizedException.class);

		verify(authEventService).record(eq(user.getId()), eq("carol@x.io"), eq(DmitriyAuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_happyPath_issuesPairAndRecordsSuccess() {
		DmitriyUserEntity user = activeUser("dan@x.io");
		when(userLookupService.findByEmailOrNull("dan@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

		DmitriyTokenPairResponse pair = authService.login("dan@x.io", "pw", now, "ip", "ua");

		assertThat(pair.accessToken()).isEqualTo("access.jwt.token");
		assertThat(pair.refreshToken()).contains("secret-abc");
		verify(refreshTokenRepository).save(any(DmitriyRefreshTokenEntity.class));
		verify(authEventService).record(eq(user.getId()), eq("dan@x.io"), eq(DmitriyAuthEventType.LOGIN_SUCCESS), eq("ip"), eq("ua"));
	}

	@Test
	void refresh_malformed_throws() {
		when(tokenService.parseRefreshToken("garbage")).thenThrow(new IllegalArgumentException("bad"));

		assertThatThrownBy(() -> authService.refresh("garbage", now, null, null))
				.isInstanceOf(DmitriyUnauthorizedException.class);
	}

	@Test
	void refresh_happyPath_revokesOldIssuesNewRecordsEvent() {
		DmitriyUserEntity user = activeUser("e@x.io");
		UUID tokenId = UUID.randomUUID();
		DmitriyRefreshTokenEntity stored = new DmitriyRefreshTokenEntity(tokenId, user, "secret-hash", now.plus(Duration.ofDays(10)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new DmitriyTokenService.ParsedRefreshToken(tokenId, "secret"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));
		when(passwordEncoder.matches("secret", "secret-hash")).thenReturn(true);

		DmitriyTokenPairResponse pair = authService.refresh("raw", now, "ip", "ua");

		assertThat(pair.accessToken()).isNotBlank();
		assertThat(stored.isRevoked()).isTrue();
		verify(authEventService).record(eq(user.getId()), eq("e@x.io"), eq(DmitriyAuthEventType.REFRESH), eq("ip"), eq("ua"));
	}

	@Test
	void refresh_revokedToken_throws() {
		DmitriyUserEntity user = activeUser("f@x.io");
		UUID tokenId = UUID.randomUUID();
		DmitriyRefreshTokenEntity stored = new DmitriyRefreshTokenEntity(tokenId, user, "h", now.plus(Duration.ofDays(1)));
		stored.setRevokedAt(now.minus(Duration.ofMinutes(1)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new DmitriyTokenService.ParsedRefreshToken(tokenId, "s"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh("raw", now, null, null))
				.isInstanceOf(DmitriyUnauthorizedException.class);
	}

	@Test
	void logout_revokesTokenAndRecordsEvent() {
		DmitriyUserEntity user = activeUser("g@x.io");
		UUID tokenId = UUID.randomUUID();
		DmitriyRefreshTokenEntity stored = new DmitriyRefreshTokenEntity(tokenId, user, "h", now.plus(Duration.ofDays(1)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new DmitriyTokenService.ParsedRefreshToken(tokenId, "s"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));

		authService.logout("raw", now, "ip", "ua");

		assertThat(stored.isRevoked()).isTrue();
		verify(authEventService).record(eq(user.getId()), eq("g@x.io"), eq(DmitriyAuthEventType.LOGOUT), eq("ip"), eq("ua"));
	}

	@Test
	void logout_malformedToken_noop() {
		when(tokenService.parseRefreshToken("bad")).thenThrow(new IllegalArgumentException());

		authService.logout("bad", now, null, null);

		verify(refreshTokenRepository, never()).save(any());
		verify(authEventService, never()).record(any(), any(), any(), any(), any());
	}
}
