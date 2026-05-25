package com.stylish.auth.dto;

public record DmitriyTokenPairResponse(
		String tokenType,
		String accessToken,
		String refreshToken,
		long accessExpiresInSeconds
) {
	public static DmitriyTokenPairResponse bearer(String accessToken, String refreshToken, long accessExpiresInSeconds) {
		return new DmitriyTokenPairResponse("Bearer", accessToken, refreshToken, accessExpiresInSeconds);
	}
}

