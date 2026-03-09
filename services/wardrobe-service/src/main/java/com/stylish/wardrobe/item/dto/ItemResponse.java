package com.stylish.wardrobe.item.dto;

import java.time.Instant;
import java.util.UUID;

import com.stylish.wardrobe.item.ItemType;

public record ItemResponse(
		UUID id,
		String name,
		String color,
		ItemType type,
		String imageObjectKey,
		Instant createdAt
) {
}

