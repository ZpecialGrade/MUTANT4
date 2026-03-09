package com.stylish.auth.dto;

import java.util.UUID;

public record MeResponse(
		UUID userId,
		String email
) {
}

