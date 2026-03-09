package com.stylish.auth.dto;

public record TokenPairResponse(
		String tokenType,
		String accessToken,
		String refreshToken,
		long accessExpiresInSeconds
) {
	public static TokenPairResponse bearer(String accessToken, String refreshToken, long accessExpiresInSeconds) {
		return new TokenPairResponse("Bearer", accessToken, refreshToken, accessExpiresInSeconds);
	}
}

