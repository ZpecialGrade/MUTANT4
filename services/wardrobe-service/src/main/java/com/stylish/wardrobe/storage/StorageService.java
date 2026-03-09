package com.stylish.wardrobe.storage;

import java.io.InputStream;
import java.util.Optional;

public interface StorageService {
	String putObject(String objectKey, String contentType, InputStream input, long contentLength);

	StoredObject getObject(String objectKey);

	void deleteObject(String objectKey);

	record StoredObject(String objectKey, String contentType, long contentLength, InputStream inputStream) {
	}
}

