package com.stylish.wardrobe.look.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DmitriyUpdateLookRequest(
		@NotBlank @Size(max = 120) String name
) {
}

