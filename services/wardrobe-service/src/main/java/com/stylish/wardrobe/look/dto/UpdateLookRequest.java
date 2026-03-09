package com.stylish.wardrobe.look.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLookRequest(
		@NotBlank @Size(max = 120) String name
) {
}

