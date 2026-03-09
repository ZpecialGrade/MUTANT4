package com.stylish.wardrobe.profile;

import com.stylish.wardrobe.profile.dto.CreateProfileRequest;
import com.stylish.wardrobe.profile.dto.ProfileResponse;
import com.stylish.wardrobe.security.CurrentUser;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
public class ProfileController {
	private final CurrentUser currentUser;
	private final ProfileService profileService;
	private final ProfileMapper profileMapper;

	public ProfileController(CurrentUser currentUser, ProfileService profileService, ProfileMapper profileMapper) {
		this.currentUser = currentUser;
		this.profileService = profileService;
		this.profileMapper = profileMapper;
	}

	@PostMapping
	public ProfileResponse create(@Valid @RequestBody CreateProfileRequest req) {
		return profileMapper.toResponse(profileService.createProfile(currentUser.userId(), req.displayName()));
	}

	@GetMapping("/me")
	public ProfileResponse me() {
		return profileMapper.toResponse(profileService.getProfileByUserId(currentUser.userId()));
	}
}

