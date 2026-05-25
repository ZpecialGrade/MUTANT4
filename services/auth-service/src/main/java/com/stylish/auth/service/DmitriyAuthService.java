package com.stylish.auth.service;

import java.time.Instant;
import java.util.Locale;
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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DmitriyAuthService {
	private final DmitriyUserRepository userRepository;
	private final DmitriyRefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final DmitriyTokenService tokenService;
	private final DmitriyJwtProperties jwtProperties;
	private final DmitriyAuthEventService authEventService;
	private final DmitriyUserLookupService userLookupService;

	public DmitriyAuthService(
			DmitriyUserRepository userRepository,
			DmitriyRefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			DmitriyTokenService tokenService,
			DmitriyJwtProperties jwtProperties,
			DmitriyAuthEventService authEventService,
			DmitriyUserLookupService userLookupService
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.jwtProperties = jwtProperties;
		this.authEventService = authEventService;
		this.userLookupService = userLookupService;
	}

	@Transactional
	public DmitriyUserResponse register(String email, String password, String ipAddress, String userAgent) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new DmitriyConflictException("Email already registered");
		}
		String hash = passwordEncoder.encode(password);
		DmitriyUserEntity user = new DmitriyUserEntity(normalizedEmail, hash);
		DmitriyUserEntity saved = userRepository.save(user);
		authEventService.record(saved.getId(), saved.getEmail(), DmitriyAuthEventType.REGISTER, ipAddress, userAgent);
		return new DmitriyUserResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
	}

	@Transactional
	public DmitriyTokenPairResponse login(String email, String password, Instant now, String ipAddress, String userAgent) {
		String normalized = normalizeEmail(email);
		DmitriyUserEntity user = userLookupService.findByEmailOrNull(normalized);
		if (user == null) {
			authEventService.record(null, normalized, DmitriyAuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new DmitriyUnauthorizedException("Invalid credentials");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			authEventService.record(user.getId(), user.getEmail(), DmitriyAuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new DmitriyUnauthorizedException("Invalid credentials");
		}
		if (user.getStatus() != DmitriyUserStatus.ACTIVE) {
			authEventService.record(user.getId(), user.getEmail(), DmitriyAuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new DmitriyUnauthorizedException("Account is disabled");
		}
		DmitriyTokenPairResponse pair = issueTokenPair(user, now);
		authEventService.record(user.getId(), user.getEmail(), DmitriyAuthEventType.LOGIN_SUCCESS, ipAddress, userAgent);
		return pair;
	}

	@Transactional
	public DmitriyTokenPairResponse refresh(String refreshToken, Instant now, String ipAddress, String userAgent) {
		DmitriyTokenService.ParsedRefreshToken parsed;
		try {
			parsed = tokenService.parseRefreshToken(refreshToken);
		} catch (Exception e) {
			throw new DmitriyUnauthorizedException("Invalid refresh token");
		}

		DmitriyRefreshTokenEntity stored = refreshTokenRepository.findById(parsed.id())
				.orElseThrow(() -> new DmitriyUnauthorizedException("Invalid refresh token"));
		if (stored.isRevoked() || stored.isExpired(now)) {
			throw new DmitriyUnauthorizedException("Refresh token expired or revoked");
		}
		if (!passwordEncoder.matches(parsed.secret(), stored.getSecretHash())) {
			throw new DmitriyUnauthorizedException("Invalid refresh token");
		}
		if (stored.getUser().getStatus() != DmitriyUserStatus.ACTIVE) {
			throw new DmitriyUnauthorizedException("Account is disabled");
		}

		stored.setRevokedAt(now);
		refreshTokenRepository.save(stored);
		DmitriyTokenPairResponse pair = issueTokenPair(stored.getUser(), now);
		authEventService.record(stored.getUser().getId(), stored.getUser().getEmail(), DmitriyAuthEventType.REFRESH, ipAddress, userAgent);
		return pair;
	}

	@Transactional
	public void logout(String refreshToken, Instant now, String ipAddress, String userAgent) {
		DmitriyTokenService.ParsedRefreshToken parsed;
		try {
			parsed = tokenService.parseRefreshToken(refreshToken);
		} catch (Exception e) {
			return;
		}
		refreshTokenRepository.findById(parsed.id()).ifPresent(rt -> {
			if (!rt.isRevoked()) {
				rt.setRevokedAt(now);
				refreshTokenRepository.save(rt);
				authEventService.record(rt.getUser().getId(), rt.getUser().getEmail(), DmitriyAuthEventType.LOGOUT, ipAddress, userAgent);
			}
		});
	}

	private DmitriyTokenPairResponse issueTokenPair(DmitriyUserEntity user, Instant now) {
		String accessToken = tokenService.createAccessToken(user, now);
		DmitriyTokenService.RefreshTokenValue refresh = tokenService.newRefreshTokenValue();
		Instant refreshExpiresAt = now.plus(jwtProperties.refreshTtl());
		String secretHash = passwordEncoder.encode(refresh.secret());
		refreshTokenRepository.save(new DmitriyRefreshTokenEntity(refresh.id(), user, secretHash, refreshExpiresAt));
		return DmitriyTokenPairResponse.bearer(accessToken, refresh.asTokenString(), tokenService.accessExpiresInSeconds(now));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

