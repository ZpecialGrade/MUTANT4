package com.stylish.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.stylish.auth.api.ConflictException;
import com.stylish.auth.api.UnauthorizedException;
import com.stylish.auth.config.JwtProperties;
import com.stylish.auth.dto.TokenPairResponse;
import com.stylish.auth.dto.UserResponse;
import com.stylish.auth.token.RefreshTokenEntity;
import com.stylish.auth.token.RefreshTokenRepository;
import com.stylish.auth.user.UserEntity;
import com.stylish.auth.user.UserRepository;

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

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			TokenService tokenService,
			JwtProperties jwtProperties
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public UserResponse register(String email, String password) {
		String normalizedEmail = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ConflictException("Email already registered");
		}
		String hash = passwordEncoder.encode(password);
		UserEntity user = new UserEntity(normalizedEmail, hash);
		UserEntity saved = userRepository.save(user);
		return new UserResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
	}

	@Transactional
	public TokenPairResponse login(String email, String password, Instant now) {
		UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid credentials");
		}
		return issueTokenPair(user, now);
	}

	@Transactional
	public TokenPairResponse refresh(String refreshToken, Instant now) {
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

		stored.setRevokedAt(now);
		refreshTokenRepository.save(stored);
		return issueTokenPair(stored.getUser(), now);
	}

	@Transactional
	public void logout(String refreshToken, Instant now) {
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

