package com.stylish.wardrobe.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record DmitriyProfileResponse(
		UUID id,
		UUID userId,
		String displayName,
		Instant createdAt
) {
}

