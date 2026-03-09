package com.stylish.wardrobe.item.dto;

import com.stylish.wardrobe.item.ItemType;

import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
		@Size(max = 120) String name,
		@Size(max = 40) String color,
		ItemType type
) {
}

