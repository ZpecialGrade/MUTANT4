package com.stylish.auth.api;

import org.springframework.http.HttpStatus;

public class DmitriyApiException extends RuntimeException {
	private final DmitriyErrorCode errorCode;
	private final HttpStatus status;

	public DmitriyApiException(HttpStatus status, DmitriyErrorCode errorCode, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}

	public DmitriyErrorCode getErrorCode() {
		return errorCode;
	}

	public HttpStatus getStatus() {
		return status;
	}
}

