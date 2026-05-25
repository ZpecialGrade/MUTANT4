package com.stylish.wardrobe.photo;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyBadRequestException;
import com.stylish.wardrobe.api.DmitriyNotFoundException;
import com.stylish.wardrobe.profile.DmitriyProfileEntity;
import com.stylish.wardrobe.profile.DmitriyProfileService;
import com.stylish.wardrobe.storage.DmitriyObjectKeyFactory;
import com.stylish.wardrobe.storage.DmitriyStorageService;

@Service
public class DmitriyUserPhotoService {
	private final DmitriyProfileService profileService;
	private final DmitriyUserPhotoRepository userPhotoRepository;
	private final DmitriyStorageService storageService;
	private final DmitriyObjectKeyFactory objectKeyFactory;
	private final DmitriyActivityService activityService;

	public DmitriyUserPhotoService(
			DmitriyProfileService profileService,
			DmitriyUserPhotoRepository userPhotoRepository,
			DmitriyStorageService storageService,
			DmitriyObjectKeyFactory objectKeyFactory,
			DmitriyActivityService activityService
	) {
		this.profileService = profileService;
		this.userPhotoRepository = userPhotoRepository;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
		this.activityService = activityService;
	}

	@Transactional
	public DmitriyUserPhotoEntity create(UUID userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new DmitriyBadRequestException("File is required");
		}
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		String objectKey = objectKeyFactory.userPhotoKey(profile.getId(), file.getOriginalFilename());

		try (InputStream is = file.getInputStream()) {
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			storageService.putObject(objectKey, contentType, is, file.getSize());
		} catch (IOException e) {
			throw new DmitriyBadRequestException("Failed to read uploaded file");
		}

		DmitriyUserPhotoEntity saved = userPhotoRepository.save(new DmitriyUserPhotoEntity(profile, objectKey));
		activityService.record(userId, DmitriyActivityType.USER_PHOTO_UPLOADED, saved.getId(), null);
		return saved;
	}

	@Transactional(readOnly = true)
	public DmitriyUserPhotoEntity get(UUID userId, UUID photoId) {
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		return userPhotoRepository.findByIdAndProfile_Id(photoId, profile.getId())
				.orElseThrow(() -> new DmitriyNotFoundException("User photo not found"));
	}
}

