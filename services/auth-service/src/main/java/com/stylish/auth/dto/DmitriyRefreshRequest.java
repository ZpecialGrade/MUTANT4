package com.stylish.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DmitriyRefreshRequest(
		@NotBlank String refreshToken
) {
}

