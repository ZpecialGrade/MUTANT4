package com.stylish.wardrobe.api;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
	}
}

