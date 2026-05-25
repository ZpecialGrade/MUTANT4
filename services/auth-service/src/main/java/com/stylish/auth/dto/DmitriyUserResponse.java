package com.stylish.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record DmitriyUserResponse(
		UUID id,
		String email,
		Instant createdAt
) {
}

