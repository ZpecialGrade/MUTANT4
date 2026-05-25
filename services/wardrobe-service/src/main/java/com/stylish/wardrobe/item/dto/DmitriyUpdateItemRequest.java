package com.stylish.wardrobe.item.dto;

import com.stylish.wardrobe.item.DmitriyItemType;

import jakarta.validation.constraints.Size;

public record DmitriyUpdateItemRequest(
		@Size(max = 120) String name,
		@Size(max = 40) String color,
		DmitriyItemType type
) {
}

