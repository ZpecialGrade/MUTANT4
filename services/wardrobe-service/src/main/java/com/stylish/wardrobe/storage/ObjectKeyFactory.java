package com.stylish.wardrobe.storage;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class ObjectKeyFactory {
	public String itemImageKey(UUID profileId, String originalFilename) {
		return "items/" + profileId + "/" + UUID.randomUUID() + ext(originalFilename);
	}

	public String userPhotoKey(UUID profileId, String originalFilename) {
		return "user-photos/" + profileId + "/" + UUID.randomUUID() + ext(originalFilename);
	}

	public String lookResultKey(UUID profileId, UUID lookId) {
		return "looks/" + profileId + "/" + lookId + ".png";
	}

	private String ext(String originalFilename) {
		if (originalFilename == null) {
			return ".bin";
		}
		int i = originalFilename.lastIndexOf('.');
		if (i < 0 || i == originalFilename.length() - 1) {
			return ".bin";
		}
		String e = originalFilename.substring(i).toLowerCase(Locale.ROOT);
		if (e.length() > 10) {
			return ".bin";
		}
		return e;
	}
}

