package com.stylish.wardrobe.look.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DmitriyLookResponse(
		UUID id,
		String name,
		UUID sourceUserPhotoId,
		List<UUID> itemIds,
		String resultImageObjectKey,
		String resultImageUrl,
		Instant createdAt
) {
}

