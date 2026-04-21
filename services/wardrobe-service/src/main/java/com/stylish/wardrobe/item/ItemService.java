package com.stylish.wardrobe.item;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stylish.wardrobe.activity.ActivityService;
import com.stylish.wardrobe.activity.ActivityType;
import com.stylish.wardrobe.api.BadRequestException;
import com.stylish.wardrobe.api.NotFoundException;
import com.stylish.wardrobe.item.dto.CreateItemMetadata;
import com.stylish.wardrobe.item.dto.UpdateItemRequest;
import com.stylish.wardrobe.profile.ProfileEntity;
import com.stylish.wardrobe.profile.ProfileService;
import com.stylish.wardrobe.storage.ObjectKeyFactory;
import com.stylish.wardrobe.storage.StorageService;

@Service
public class ItemService {
	private final ProfileService profileService;
	private final ItemRepository itemRepository;
	private final StorageService storageService;
	private final ObjectKeyFactory objectKeyFactory;
	private final ActivityService activityService;

	public ItemService(
			ProfileService profileService,
			ItemRepository itemRepository,
			StorageService storageService,
			ObjectKeyFactory objectKeyFactory,
			ActivityService activityService
	) {
		this.profileService = profileService;
		this.itemRepository = itemRepository;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
		this.activityService = activityService;
	}

	@Transactional
	public ItemEntity create(UUID userId, CreateItemMetadata meta, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("File is required");
		}
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		String objectKey = objectKeyFactory.itemImageKey(profile.getId(), file.getOriginalFilename());

		try (InputStream is = file.getInputStream()) {
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			storageService.putObject(objectKey, contentType, is, file.getSize());
		} catch (IOException e) {
			throw new BadRequestException("Failed to read uploaded file");
		}

		ItemEntity item = new ItemEntity(profile, meta.name(), meta.color(), meta.type(), objectKey);
		ItemEntity saved = itemRepository.save(item);
		activityService.record(userId, ActivityType.ITEM_CREATED, saved.getId(), saved.getName());
		return saved;
	}

	@Transactional(readOnly = true)
	public ItemEntity get(UUID userId, UUID itemId) {
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		return itemRepository.findByIdAndProfile_Id(itemId, profile.getId())
				.orElseThrow(() -> new NotFoundException("Item not found"));
	}

	@Transactional
	public ItemEntity update(UUID userId, UUID itemId, UpdateItemRequest req) {
		ItemEntity item = get(userId, itemId);
		if (req.name() != null) {
			item.setName(req.name());
		}
		if (req.color() != null) {
			item.setColor(req.color());
		}
		if (req.type() != null) {
			item.setType(req.type());
		}
		ItemEntity saved = itemRepository.save(item);
		activityService.record(userId, ActivityType.ITEM_UPDATED, saved.getId(), saved.getName());
		return saved;
	}

	@Transactional
	public void delete(UUID userId, UUID itemId) {
		ItemEntity item = get(userId, itemId);
		UUID targetId = item.getId();
		String name = item.getName();
		itemRepository.delete(item);
		storageService.deleteObject(item.getImageObjectKey());
		activityService.record(userId, ActivityType.ITEM_DELETED, targetId, name);
	}

	@Transactional(readOnly = true)
	public Page<ItemEntity> list(UUID userId, ItemType type, String color, String nameLike, Pageable pageable) {
		ProfileEntity profile = profileService.getProfileByUserId(userId);

		Specification<ItemEntity> spec = Specification.where(ItemSpecifications.profileId(profile.getId()));
		if (type != null) {
			spec = spec.and(ItemSpecifications.type(type));
		}
		if (color != null && !color.isBlank()) {
			spec = spec.and(ItemSpecifications.colorEqualsIgnoreCase(color.trim()));
		}
		if (nameLike != null && !nameLike.isBlank()) {
			spec = spec.and(ItemSpecifications.nameLikeIgnoreCase(nameLike.trim()));
		}
		return itemRepository.findAll(spec, pageable);
	}
}

