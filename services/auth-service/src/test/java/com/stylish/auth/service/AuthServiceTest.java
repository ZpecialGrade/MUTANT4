package com.stylish.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.stylish.auth.api.ConflictException;
import com.stylish.auth.api.UnauthorizedException;
import com.stylish.auth.config.JwtProperties;
import com.stylish.auth.dto.TokenPairResponse;
import com.stylish.auth.dto.UserResponse;
import com.stylish.auth.event.AuthEventService;
import com.stylish.auth.event.AuthEventType;
import com.stylish.auth.token.RefreshTokenEntity;
import com.stylish.auth.token.RefreshTokenRepository;
import com.stylish.auth.user.UserEntity;
import com.stylish.auth.user.UserLookupService;
import com.stylish.auth.user.UserRepository;
import com.stylish.auth.user.UserStatus;

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
class AuthServiceTest {

	@Mock UserRepository userRepository;
	@Mock RefreshTokenRepository refreshTokenRepository;
	@Mock PasswordEncoder passwordEncoder;
	@Mock TokenService tokenService;
	@Mock JwtProperties jwtProperties;
	@Mock AuthEventService authEventService;
	@Mock UserLookupService userLookupService;

	@InjectMocks AuthService authService;

	private final Instant now = Instant.parse("2025-01-01T00:00:00Z");

	private UserEntity activeUser(String email) {
		UserEntity u = new UserEntity(email, "hash");
		ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(u, "createdAt", now);
		ReflectionTestUtils.setField(u, "status", UserStatus.ACTIVE);
		return u;
	}

	@BeforeEach
	void setupTokenService() {
		when(jwtProperties.refreshTtl()).thenReturn(Duration.ofDays(30));
		when(tokenService.newRefreshTokenValue()).thenReturn(
				new TokenService.RefreshTokenValue(UUID.randomUUID(), "secret-abc")
		);
		when(tokenService.createAccessToken(any(), any())).thenReturn("access.jwt.token");
		when(tokenService.accessExpiresInSeconds(any())).thenReturn(900L);
		when(passwordEncoder.encode(anyString())).thenReturn("hashed");
	}

	@Test
	void register_happyPath_savesUserAndEmitsEvent() {
		when(userRepository.existsByEmailIgnoreCase("alice@x.io")).thenReturn(false);
		UserEntity saved = activeUser("alice@x.io");
		when(userRepository.save(any(UserEntity.class))).thenReturn(saved);

		UserResponse resp = authService.register("Alice@X.io", "pw", "1.1.1.1", "ua");

		assertThat(resp.email()).isEqualTo("alice@x.io");
		verify(authEventService).record(eq(saved.getId()), eq("alice@x.io"), eq(AuthEventType.REGISTER), eq("1.1.1.1"), eq("ua"));
	}

	@Test
	void register_duplicateEmail_throwsConflict() {
		when(userRepository.existsByEmailIgnoreCase("dup@x.io")).thenReturn(true);

		assertThatThrownBy(() -> authService.register("dup@x.io", "pw", null, null))
				.isInstanceOf(ConflictException.class);

		verify(userRepository, never()).save(any());
		verify(authEventService, never()).record(any(), any(), any(), any(), any());
	}

	@Test
	void login_unknownEmail_throwsAndRecordsFailed() {
		when(userLookupService.findByEmailOrNull("ghost@x.io")).thenReturn(null);

		assertThatThrownBy(() -> authService.login("ghost@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(UnauthorizedException.class);

		verify(authEventService).record(eq(null), eq("ghost@x.io"), eq(AuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_wrongPassword_throwsAndRecordsFailed() {
		UserEntity user = activeUser("bob@x.io");
		when(userLookupService.findByEmailOrNull("bob@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login("bob@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(UnauthorizedException.class);

		verify(authEventService).record(eq(user.getId()), eq("bob@x.io"), eq(AuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_disabled_throwsAndRecordsFailed() {
		UserEntity user = activeUser("carol@x.io");
		ReflectionTestUtils.setField(user, "status", UserStatus.DISABLED);
		when(userLookupService.findByEmailOrNull("carol@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

		assertThatThrownBy(() -> authService.login("carol@x.io", "pw", now, "ip", "ua"))
				.isInstanceOf(UnauthorizedException.class);

		verify(authEventService).record(eq(user.getId()), eq("carol@x.io"), eq(AuthEventType.LOGIN_FAILED), eq("ip"), eq("ua"));
	}

	@Test
	void login_happyPath_issuesPairAndRecordsSuccess() {
		UserEntity user = activeUser("dan@x.io");
		when(userLookupService.findByEmailOrNull("dan@x.io")).thenReturn(user);
		when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

		TokenPairResponse pair = authService.login("dan@x.io", "pw", now, "ip", "ua");

		assertThat(pair.accessToken()).isEqualTo("access.jwt.token");
		assertThat(pair.refreshToken()).contains("secret-abc");
		verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
		verify(authEventService).record(eq(user.getId()), eq("dan@x.io"), eq(AuthEventType.LOGIN_SUCCESS), eq("ip"), eq("ua"));
	}

	@Test
	void refresh_malformed_throws() {
		when(tokenService.parseRefreshToken("garbage")).thenThrow(new IllegalArgumentException("bad"));

		assertThatThrownBy(() -> authService.refresh("garbage", now, null, null))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void refresh_happyPath_revokesOldIssuesNewRecordsEvent() {
		UserEntity user = activeUser("e@x.io");
		UUID tokenId = UUID.randomUUID();
		RefreshTokenEntity stored = new RefreshTokenEntity(tokenId, user, "secret-hash", now.plus(Duration.ofDays(10)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new TokenService.ParsedRefreshToken(tokenId, "secret"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));
		when(passwordEncoder.matches("secret", "secret-hash")).thenReturn(true);

		TokenPairResponse pair = authService.refresh("raw", now, "ip", "ua");

		assertThat(pair.accessToken()).isNotBlank();
		assertThat(stored.isRevoked()).isTrue();
		verify(authEventService).record(eq(user.getId()), eq("e@x.io"), eq(AuthEventType.REFRESH), eq("ip"), eq("ua"));
	}

	@Test
	void refresh_revokedToken_throws() {
		UserEntity user = activeUser("f@x.io");
		UUID tokenId = UUID.randomUUID();
		RefreshTokenEntity stored = new RefreshTokenEntity(tokenId, user, "h", now.plus(Duration.ofDays(1)));
		stored.setRevokedAt(now.minus(Duration.ofMinutes(1)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new TokenService.ParsedRefreshToken(tokenId, "s"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));

		assertThatThrownBy(() -> authService.refresh("raw", now, null, null))
				.isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void logout_revokesTokenAndRecordsEvent() {
		UserEntity user = activeUser("g@x.io");
		UUID tokenId = UUID.randomUUID();
		RefreshTokenEntity stored = new RefreshTokenEntity(tokenId, user, "h", now.plus(Duration.ofDays(1)));

		when(tokenService.parseRefreshToken("raw")).thenReturn(new TokenService.ParsedRefreshToken(tokenId, "s"));
		when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(stored));

		authService.logout("raw", now, "ip", "ua");

		assertThat(stored.isRevoked()).isTrue();
		verify(authEventService).record(eq(user.getId()), eq("g@x.io"), eq(AuthEventType.LOGOUT), eq("ip"), eq("ua"));
	}

	@Test
	void logout_malformedToken_noop() {
		when(tokenService.parseRefreshToken("bad")).thenThrow(new IllegalArgumentException());

		authService.logout("bad", now, null, null);

		verify(refreshTokenRepository, never()).save(any());
		verify(authEventService, never()).record(any(), any(), any(), any(), any());
	}
}
