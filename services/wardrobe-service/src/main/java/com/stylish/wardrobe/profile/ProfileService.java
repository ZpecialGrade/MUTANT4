package com.stylish.wardrobe.profile;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stylish.wardrobe.api.ConflictException;
import com.stylish.wardrobe.api.NotFoundException;

@Service
public class ProfileService {
	private final ProfileRepository profileRepository;

	public ProfileService(ProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
	}

	@Transactional
	public ProfileEntity createProfile(UUID userId, String displayName) {
		if (profileRepository.existsByUserId(userId)) {
			throw new ConflictException("Profile already exists");
		}
		return profileRepository.save(new ProfileEntity(userId, displayName));
	}

	@Transactional(readOnly = true)
	public ProfileEntity getProfileByUserId(UUID userId) {
		return profileRepository.findByUserId(userId)
				.orElseThrow(() -> new NotFoundException("Profile not found"));
	}
}

