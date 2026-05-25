package com.stylish.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DmitriyLogoutRequest(
		@NotBlank String refreshToken
) {
}

