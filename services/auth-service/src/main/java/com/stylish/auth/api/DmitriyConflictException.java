package com.stylish.auth.api;

import org.springframework.http.HttpStatus;

public class DmitriyConflictException extends DmitriyApiException {
	public DmitriyConflictException(String message) {
		super(HttpStatus.CONFLICT, DmitriyErrorCode.CONFLICT, message);
	}
}

