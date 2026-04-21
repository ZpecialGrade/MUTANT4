package com.stylish.auth.service;

import java.time.Instant;
import java.util.Locale;
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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final JwtProperties jwtProperties;
	private final AuthEventService authEventService;
	private final UserLookupService userLookupService;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			TokenService tokenService,
			JwtProperties jwtProperties,
			AuthEventService authEventService,
			UserLookupService userLookupService
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
	public UserResponse register(String email, String password, String ipAddress, String userAgent) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ConflictException("Email already registered");
		}
		String hash = passwordEncoder.encode(password);
		UserEntity user = new UserEntity(normalizedEmail, hash);
		UserEntity saved = userRepository.save(user);
		authEventService.record(saved.getId(), saved.getEmail(), AuthEventType.REGISTER, ipAddress, userAgent);
		return new UserResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
	}

	@Transactional
	public TokenPairResponse login(String email, String password, Instant now, String ipAddress, String userAgent) {
		String normalized = normalizeEmail(email);
		UserEntity user = userLookupService.findByEmailOrNull(normalized);
		if (user == null) {
			authEventService.record(null, normalized, AuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new UnauthorizedException("Invalid credentials");
		}
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			authEventService.record(user.getId(), user.getEmail(), AuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new UnauthorizedException("Invalid credentials");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			authEventService.record(user.getId(), user.getEmail(), AuthEventType.LOGIN_FAILED, ipAddress, userAgent);
			throw new UnauthorizedException("Account is disabled");
		}
		TokenPairResponse pair = issueTokenPair(user, now);
		authEventService.record(user.getId(), user.getEmail(), AuthEventType.LOGIN_SUCCESS, ipAddress, userAgent);
		return pair;
	}

	@Transactional
	public TokenPairResponse refresh(String refreshToken, Instant now, String ipAddress, String userAgent) {
		TokenService.ParsedRefreshToken parsed;
		try {
			parsed = tokenService.parseRefreshToken(refreshToken);
		} catch (Exception e) {
			throw new UnauthorizedException("Invalid refresh token");
		}

		RefreshTokenEntity stored = refreshTokenRepository.findById(parsed.id())
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
		if (stored.isRevoked() || stored.isExpired(now)) {
			throw new UnauthorizedException("Refresh token expired or revoked");
		}
		if (!passwordEncoder.matches(parsed.secret(), stored.getSecretHash())) {
			throw new UnauthorizedException("Invalid refresh token");
		}
		if (stored.getUser().getStatus() != UserStatus.ACTIVE) {
			throw new UnauthorizedException("Account is disabled");
		}

		stored.setRevokedAt(now);
		refreshTokenRepository.save(stored);
		TokenPairResponse pair = issueTokenPair(stored.getUser(), now);
		authEventService.record(stored.getUser().getId(), stored.getUser().getEmail(), AuthEventType.REFRESH, ipAddress, userAgent);
		return pair;
	}

	@Transactional
	public void logout(String refreshToken, Instant now, String ipAddress, String userAgent) {
		TokenService.ParsedRefreshToken parsed;
		try {
			parsed = tokenService.parseRefreshToken(refreshToken);
		} catch (Exception e) {
			return;
		}
		refreshTokenRepository.findById(parsed.id()).ifPresent(rt -> {
			if (!rt.isRevoked()) {
				rt.setRevokedAt(now);
				refreshTokenRepository.save(rt);
				authEventService.record(rt.getUser().getId(), rt.getUser().getEmail(), AuthEventType.LOGOUT, ipAddress, userAgent);
			}
		});
	}

	private TokenPairResponse issueTokenPair(UserEntity user, Instant now) {
		String accessToken = tokenService.createAccessToken(user, now);
		TokenService.RefreshTokenValue refresh = tokenService.newRefreshTokenValue();
		Instant refreshExpiresAt = now.plus(jwtProperties.refreshTtl());
		String secretHash = passwordEncoder.encode(refresh.secret());
		refreshTokenRepository.save(new RefreshTokenEntity(refresh.id(), user, secretHash, refreshExpiresAt));
		return TokenPairResponse.bearer(accessToken, refresh.asTokenString(), tokenService.accessExpiresInSeconds(now));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

