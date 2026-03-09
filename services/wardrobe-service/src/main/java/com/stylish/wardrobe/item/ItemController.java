package com.stylish.wardrobe.item;

import java.util.UUID;

import com.stylish.wardrobe.item.dto.CreateItemMetadata;
import com.stylish.wardrobe.item.dto.ItemResponse;
import com.stylish.wardrobe.item.dto.UpdateItemRequest;
import com.stylish.wardrobe.security.CurrentUser;

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
public class ItemController {
	private final CurrentUser currentUser;
	private final ItemService itemService;
	private final ItemMapper itemMapper;

	public ItemController(CurrentUser currentUser, ItemService itemService, ItemMapper itemMapper) {
		this.currentUser = currentUser;
		this.itemService = itemService;
		this.itemMapper = itemMapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ItemResponse create(
			@Valid @RequestPart("metadata") CreateItemMetadata metadata,
			@RequestPart("file") MultipartFile file
	) {
		return itemMapper.toResponse(itemService.create(currentUser.userId(), metadata, file));
	}

	@GetMapping("/{id}")
	public ItemResponse get(@PathVariable UUID id) {
		return itemMapper.toResponse(itemService.get(currentUser.userId(), id));
	}

	@GetMapping
	public Page<ItemResponse> list(
			@RequestParam(required = false) ItemType type,
			@RequestParam(required = false) String color,
			@RequestParam(required = false) String nameLike,
			Pageable pageable
	) {
		return itemService.list(currentUser.userId(), type, color, nameLike, pageable).map(itemMapper::toResponse);
	}

	@PatchMapping("/{id}")
	public ItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest req) {
		return itemMapper.toResponse(itemService.update(currentUser.userId(), id, req));
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable UUID id) {
		itemService.delete(currentUser.userId(), id);
	}
}

