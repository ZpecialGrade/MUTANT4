package com.stylish.wardrobe.look.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DmitriyGenerateLookRequest(
		@NotNull UUID userPhotoId,
		@NotEmpty List<@NotNull UUID> itemIds,
		@Size(max = 120) String name
) {
}

