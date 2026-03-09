package com.stylish.wardrobe.photo.dto;

import java.time.Instant;
import java.util.UUID;

public record UserPhotoResponse(
		UUID id,
		String imageObjectKey,
		Instant createdAt
) {
}

