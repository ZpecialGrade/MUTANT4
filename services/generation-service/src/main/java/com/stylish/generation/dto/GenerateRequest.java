package com.stylish.generation.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GenerateRequest(
		@NotBlank String userPhotoObjectKey,
		@NotNull List<@NotBlank String> itemObjectKeys
) {
}

