package com.stylish.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.stylish.auth.config.DmitriyJwtProperties;
import com.stylish.auth.user.DmitriyUserEntity;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class DmitriyTokenService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

	private final DmitriyJwtProperties jwtProperties;
	private final JwtEncoder jwtEncoder;

	public DmitriyTokenService(DmitriyJwtProperties jwtProperties, JwtEncoder jwtEncoder) {
		this.jwtProperties = jwtProperties;
		this.jwtEncoder = jwtEncoder;
	}

	public String createAccessToken(DmitriyUserEntity user, Instant now) {
		Instant expiresAt = now.plus(jwtProperties.accessTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuedAt(now)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long accessExpiresInSeconds(Instant now) {
		return jwtProperties.accessTtl().toSeconds();
	}

	public RefreshTokenValue newRefreshTokenValue() {
		UUID tokenId = UUID.randomUUID();
		String secret = randomUrlSafeString(32);
		return new RefreshTokenValue(tokenId, secret);
	}

	public ParsedRefreshToken parseRefreshToken(String token) {
		String[] parts = token.split("\\.", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid refresh token format");
		}
		UUID tokenId = UUID.fromString(parts[0]);
		return new ParsedRefreshToken(tokenId, parts[1]);
	}

	private String randomUrlSafeString(int bytes) {
		byte[] data = new byte[bytes];
		SECURE_RANDOM.nextBytes(data);
		return BASE64_URL.encodeToString(data);
	}

	public record RefreshTokenValue(UUID id, String secret) {
		public String asTokenString() {
			return id + "." + secret;
		}
	}

	public record ParsedRefreshToken(UUID id, String secret) {
	}
}

