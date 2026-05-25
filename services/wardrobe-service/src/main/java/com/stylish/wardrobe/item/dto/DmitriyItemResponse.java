package com.stylish.wardrobe.item.dto;

import java.time.Instant;
import java.util.UUID;

import com.stylish.wardrobe.item.DmitriyItemType;

public record DmitriyItemResponse(
		UUID id,
		String name,
		String color,
		DmitriyItemType type,
		String imageObjectKey,
		Instant createdAt
) {
}

