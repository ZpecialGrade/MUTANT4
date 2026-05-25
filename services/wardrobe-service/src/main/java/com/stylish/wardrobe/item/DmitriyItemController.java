package com.stylish.wardrobe.item;

import java.util.UUID;

import com.stylish.wardrobe.item.dto.DmitriyCreateItemMetadata;
import com.stylish.wardrobe.item.dto.DmitriyItemResponse;
import com.stylish.wardrobe.item.dto.DmitriyUpdateItemRequest;
import com.stylish.wardrobe.security.DmitriyCurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/items")
@Tag(name = "Items", description = "Вещи в гардеробе пользователя")
@SecurityRequirement(name = "bearer")
public class DmitriyItemController {
	private final DmitriyCurrentUser currentUser;
	private final DmitriyItemService itemService;
	private final DmitriyItemMapper itemMapper;

	public DmitriyItemController(DmitriyCurrentUser currentUser, DmitriyItemService itemService, DmitriyItemMapper itemMapper) {
		this.currentUser = currentUser;
		this.itemService = itemService;
		this.itemMapper = itemMapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Создать вещь с картинкой (multipart/form-data: metadata + file)")
	public DmitriyItemResponse create(
			@Valid @RequestPart("metadata") DmitriyCreateItemMetadata metadata,
			@RequestPart("file") MultipartFile file
	) {
		return itemMapper.toResponse(itemService.create(currentUser.userId(), metadata, file));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Получить вещь по id")
	public DmitriyItemResponse get(@PathVariable UUID id) {
		return itemMapper.toResponse(itemService.get(currentUser.userId(), id));
	}

	@GetMapping
	@Operation(summary = "Список вещей текущего пользователя с фильтрами и пагинацией")
	public Page<DmitriyItemResponse> list(
			@RequestParam(required = false) DmitriyItemType type,
			@RequestParam(required = false) String color,
			@RequestParam(required = false) String nameLike,
			Pageable pageable
	) {
		return itemService.list(currentUser.userId(), type, color, nameLike, pageable).map(itemMapper::toResponse);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Обновить метаданные вещи")
	public DmitriyItemResponse update(@PathVariable UUID id, @Valid @RequestBody DmitriyUpdateItemRequest req) {
		return itemMapper.toResponse(itemService.update(currentUser.userId(), id, req));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Удалить вещь (из БД и из S3)")
	public void delete(@PathVariable UUID id) {
		itemService.delete(currentUser.userId(), id);
	}
}

