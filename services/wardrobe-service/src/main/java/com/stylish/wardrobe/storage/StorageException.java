package com.stylish.wardrobe.storage;

import com.stylish.wardrobe.api.ApiException;
import com.stylish.wardrobe.api.ErrorCode;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {
	public StorageException(String message) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.STORAGE_ERROR, message);
	}

	public StorageException(String message, Exception cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.STORAGE_ERROR, message);
		initCause(cause);
	}
}

