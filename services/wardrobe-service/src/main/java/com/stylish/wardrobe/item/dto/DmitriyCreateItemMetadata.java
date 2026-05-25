package com.stylish.wardrobe.item.dto;

import com.stylish.wardrobe.item.DmitriyItemType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DmitriyCreateItemMetadata(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 40) String color,
		@NotNull DmitriyItemType type
) {
}

