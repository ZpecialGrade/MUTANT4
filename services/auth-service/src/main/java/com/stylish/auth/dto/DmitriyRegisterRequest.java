package com.stylish.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DmitriyRegisterRequest(
		@Email @NotBlank String email,
		@NotBlank @Size(min = 8, max = 72) String password
) {
}

