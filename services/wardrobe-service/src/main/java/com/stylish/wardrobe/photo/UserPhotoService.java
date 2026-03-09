package com.stylish.wardrobe.photo;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stylish.wardrobe.api.BadRequestException;
import com.stylish.wardrobe.api.NotFoundException;
import com.stylish.wardrobe.profile.ProfileEntity;
import com.stylish.wardrobe.profile.ProfileService;
import com.stylish.wardrobe.storage.ObjectKeyFactory;
import com.stylish.wardrobe.storage.StorageService;

@Service
public class UserPhotoService {
	private final ProfileService profileService;
	private final UserPhotoRepository userPhotoRepository;
	private final StorageService storageService;
	private final ObjectKeyFactory objectKeyFactory;

	public UserPhotoService(
			ProfileService profileService,
			UserPhotoRepository userPhotoRepository,
			StorageService storageService,
			ObjectKeyFactory objectKeyFactory
	) {
		this.profileService = profileService;
		this.userPhotoRepository = userPhotoRepository;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
	}

	@Transactional
	public UserPhotoEntity create(UUID userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("File is required");
		}
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		String objectKey = objectKeyFactory.userPhotoKey(profile.getId(), file.getOriginalFilename());

		try (InputStream is = file.getInputStream()) {
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			storageService.putObject(objectKey, contentType, is, file.getSize());
		} catch (IOException e) {
			throw new BadRequestException("Failed to read uploaded file");
		}

		return userPhotoRepository.save(new UserPhotoEntity(profile, objectKey));
	}

	@Transactional(readOnly = true)
	public UserPhotoEntity get(UUID userId, UUID photoId) {
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		return userPhotoRepository.findByIdAndProfile_Id(photoId, profile.getId())
				.orElseThrow(() -> new NotFoundException("User photo not found"));
	}
}

