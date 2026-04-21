package com.stylish.wardrobe.profile;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stylish.wardrobe.activity.ActivityService;
import com.stylish.wardrobe.activity.ActivityType;
import com.stylish.wardrobe.api.ConflictException;
import com.stylish.wardrobe.api.NotFoundException;
import com.stylish.wardrobe.config.CacheConfig;

@Service
public class ProfileService {
	private final ProfileRepository profileRepository;
	private final ActivityService activityService;

	public ProfileService(ProfileRepository profileRepository, ActivityService activityService) {
		this.profileRepository = profileRepository;
		this.activityService = activityService;
	}

	@Transactional
	@CacheEvict(cacheNames = CacheConfig.PROFILES_BY_USER_ID, key = "#userId")
	public ProfileEntity createProfile(UUID userId, String displayName) {
		if (profileRepository.existsByUserId(userId)) {
			throw new ConflictException("Profile already exists");
		}
		ProfileEntity saved = profileRepository.save(new ProfileEntity(userId, displayName));
		activityService.record(userId, ActivityType.PROFILE_CREATED, saved.getId(), saved.getDisplayName());
		return saved;
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = CacheConfig.PROFILES_BY_USER_ID, key = "#userId")
	public ProfileEntity getProfileByUserId(UUID userId) {
		return profileRepository.findByUserId(userId)
				.orElseThrow(() -> new NotFoundException("Profile not found"));
	}
}

