package com.stylish.wardrobe.profile;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyConflictException;
import com.stylish.wardrobe.api.DmitriyNotFoundException;
import com.stylish.wardrobe.config.DmitriyCacheConfig;

@Service
public class DmitriyProfileService {
	private final DmitriyProfileRepository profileRepository;
	private final DmitriyActivityService activityService;

	public DmitriyProfileService(DmitriyProfileRepository profileRepository, DmitriyActivityService activityService) {
		this.profileRepository = profileRepository;
		this.activityService = activityService;
	}

	@Transactional
	@CacheEvict(cacheNames = DmitriyCacheConfig.PROFILES_BY_USER_ID, key = "#userId")
	public DmitriyProfileEntity createProfile(UUID userId, String displayName) {
		if (profileRepository.existsByUserId(userId)) {
			throw new DmitriyConflictException("Profile already exists");
		}
		DmitriyProfileEntity saved = profileRepository.save(new DmitriyProfileEntity(userId, displayName));
		activityService.record(userId, DmitriyActivityType.PROFILE_CREATED, saved.getId(), saved.getDisplayName());
		return saved;
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = DmitriyCacheConfig.PROFILES_BY_USER_ID, key = "#userId")
	public DmitriyProfileEntity getProfileByUserId(UUID userId) {
		return profileRepository.findByUserId(userId)
				.orElseThrow(() -> new DmitriyNotFoundException("Profile not found"));
	}
}

