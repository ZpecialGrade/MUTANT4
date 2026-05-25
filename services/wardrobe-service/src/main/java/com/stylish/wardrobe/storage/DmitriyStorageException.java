package com.stylish.wardrobe.storage;

import com.stylish.wardrobe.api.DmitriyApiException;
import com.stylish.wardrobe.api.DmitriyErrorCode;

import org.springframework.http.HttpStatus;

public class DmitriyStorageException extends DmitriyApiException {
	public DmitriyStorageException(String message) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, DmitriyErrorCode.STORAGE_ERROR, message);
	}

	public DmitriyStorageException(String message, Exception cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, DmitriyErrorCode.STORAGE_ERROR, message);
		initCause(cause);
	}
}

