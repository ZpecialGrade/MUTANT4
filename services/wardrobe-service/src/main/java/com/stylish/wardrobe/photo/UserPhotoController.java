package com.stylish.wardrobe.photo;

import java.util.UUID;

import com.stylish.wardrobe.photo.dto.UserPhotoResponse;
import com.stylish.wardrobe.security.CurrentUser;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user-photos")
public class UserPhotoController {
	private final CurrentUser currentUser;
	private final UserPhotoService userPhotoService;
	private final UserPhotoMapper userPhotoMapper;

	public UserPhotoController(CurrentUser currentUser, UserPhotoService userPhotoService, UserPhotoMapper userPhotoMapper) {
		this.currentUser = currentUser;
		this.userPhotoService = userPhotoService;
		this.userPhotoMapper = userPhotoMapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserPhotoResponse create(@RequestPart("file") MultipartFile file) {
		return userPhotoMapper.toResponse(userPhotoService.create(currentUser.userId(), file));
	}

	@GetMapping("/{id}")
	public UserPhotoResponse get(@PathVariable UUID id) {
		return userPhotoMapper.toResponse(userPhotoService.get(currentUser.userId(), id));
	}
}

