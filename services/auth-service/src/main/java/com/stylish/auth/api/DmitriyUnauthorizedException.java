package com.stylish.auth.api;

import org.springframework.http.HttpStatus;

public class DmitriyUnauthorizedException extends DmitriyApiException {
	public DmitriyUnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, DmitriyErrorCode.UNAUTHORIZED, message);
	}
}

