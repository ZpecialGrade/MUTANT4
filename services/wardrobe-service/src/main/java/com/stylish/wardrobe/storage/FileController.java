package com.stylish.wardrobe.storage;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileController {
	private final StorageService storageService;

	public FileController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/{*objectKey}")
	public ResponseEntity<InputStreamResource> download(@PathVariable String objectKey) {
		StorageService.StoredObject obj = storageService.getObject(objectKey);
		MediaType mediaType = MediaType.parseMediaType(obj.contentType());
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
				.contentType(mediaType);
		if (obj.contentLength() >= 0) {
			builder.contentLength(obj.contentLength());
		}
		return builder.body(new InputStreamResource(obj.inputStream()));
	}
}

