package com.stylish.auth.api;

import org.springframework.http.HttpStatus;

public class DmitriyBadRequestException extends DmitriyApiException {
	public DmitriyBadRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, DmitriyErrorCode.BAD_REQUEST, message);
	}
}

