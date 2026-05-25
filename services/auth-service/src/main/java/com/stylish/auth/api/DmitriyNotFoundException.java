package com.stylish.auth.api;

import org.springframework.http.HttpStatus;

public class DmitriyNotFoundException extends DmitriyApiException {
	public DmitriyNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, DmitriyErrorCode.NOT_FOUND, message);
	}
}

