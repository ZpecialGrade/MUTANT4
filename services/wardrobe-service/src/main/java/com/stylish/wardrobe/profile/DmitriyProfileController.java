package com.stylish.wardrobe.profile;

import com.stylish.wardrobe.profile.dto.DmitriyCreateProfileRequest;
import com.stylish.wardrobe.profile.dto.DmitriyProfileResponse;
import com.stylish.wardrobe.security.DmitriyCurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles")
@Tag(name = "Profiles", description = "Профиль пользователя в гардеробе")
@SecurityRequirement(name = "bearer")
public class DmitriyProfileController {
	private final DmitriyCurrentUser currentUser;
	private final DmitriyProfileService profileService;
	private final DmitriyProfileMapper profileMapper;

	public DmitriyProfileController(DmitriyCurrentUser currentUser, DmitriyProfileService profileService, DmitriyProfileMapper profileMapper) {
		this.currentUser = currentUser;
		this.profileService = profileService;
		this.profileMapper = profileMapper;
	}

	@PostMapping
	@Operation(summary = "Создать профиль текущего пользователя")
	public DmitriyProfileResponse create(@Valid @RequestBody DmitriyCreateProfileRequest req) {
		return profileMapper.toResponse(profileService.createProfile(currentUser.userId(), req.displayName()));
	}

	@GetMapping("/me")
	@Operation(summary = "Профиль текущего пользователя")
	public DmitriyProfileResponse me() {
		return profileMapper.toResponse(profileService.getProfileByUserId(currentUser.userId()));
	}
}

