package com.stylish.wardrobe.photo;

import java.util.UUID;

import com.stylish.wardrobe.photo.dto.DmitriyUserPhotoResponse;
import com.stylish.wardrobe.security.DmitriyCurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "User Photos", description = "Фото пользователя (исходник для примерки)")
@SecurityRequirement(name = "bearer")
public class DmitriyUserPhotoController {
	private final DmitriyCurrentUser currentUser;
	private final DmitriyUserPhotoService userPhotoService;
	private final DmitriyUserPhotoMapper userPhotoMapper;

	public DmitriyUserPhotoController(DmitriyCurrentUser currentUser, DmitriyUserPhotoService userPhotoService, DmitriyUserPhotoMapper userPhotoMapper) {
		this.currentUser = currentUser;
		this.userPhotoService = userPhotoService;
		this.userPhotoMapper = userPhotoMapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Загрузить фото пользователя (multipart/form-data: file)")
	public DmitriyUserPhotoResponse create(@RequestPart("file") MultipartFile file) {
		return userPhotoMapper.toResponse(userPhotoService.create(currentUser.userId(), file));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Получить фото пользователя по id")
	public DmitriyUserPhotoResponse get(@PathVariable UUID id) {
		return userPhotoMapper.toResponse(userPhotoService.get(currentUser.userId(), id));
	}
}

