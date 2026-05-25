package com.stylish.auth.dto;

import java.util.UUID;

public record DmitriyMeResponse(
		UUID userId,
		String email
) {
}

