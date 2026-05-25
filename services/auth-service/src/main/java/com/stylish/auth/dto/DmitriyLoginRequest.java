package com.stylish.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DmitriyLoginRequest(
		@Email @NotBlank String email,
		@NotBlank String password
) {
}

