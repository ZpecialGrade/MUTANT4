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

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyBadRequestException;
import com.stylish.wardrobe.api.DmitriyNotFoundException;
import com.stylish.wardrobe.item.dto.DmitriyCreateItemMetadata;
import com.stylish.wardrobe.item.dto.DmitriyUpdateItemRequest;
import com.stylish.wardrobe.profile.DmitriyProfileEntity;
import com.stylish.wardrobe.profile.DmitriyProfileService;
import com.stylish.wardrobe.storage.DmitriyObjectKeyFactory;
import com.stylish.wardrobe.storage.DmitriyStorageService;

@Service
public class DmitriyItemService {
	private final DmitriyProfileService profileService;
	private final DmitriyItemRepository itemRepository;
	private final DmitriyStorageService storageService;
	private final DmitriyObjectKeyFactory objectKeyFactory;
	private final DmitriyActivityService activityService;

	public DmitriyItemService(
			DmitriyProfileService profileService,
			DmitriyItemRepository itemRepository,
			DmitriyStorageService storageService,
			DmitriyObjectKeyFactory objectKeyFactory,
			DmitriyActivityService activityService
	) {
		this.profileService = profileService;
		this.itemRepository = itemRepository;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
		this.activityService = activityService;
	}

	@Transactional
	public DmitriyItemEntity create(UUID userId, DmitriyCreateItemMetadata meta, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new DmitriyBadRequestException("File is required");
		}
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		String objectKey = objectKeyFactory.itemImageKey(profile.getId(), file.getOriginalFilename());

		try (InputStream is = file.getInputStream()) {
			String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
			storageService.putObject(objectKey, contentType, is, file.getSize());
		} catch (IOException e) {
			throw new DmitriyBadRequestException("Failed to read uploaded file");
		}

		DmitriyItemEntity item = new DmitriyItemEntity(profile, meta.name(), meta.color(), meta.type(), objectKey);
		DmitriyItemEntity saved = itemRepository.save(item);
		activityService.record(userId, DmitriyActivityType.ITEM_CREATED, saved.getId(), saved.getName());
		return saved;
	}

	@Transactional(readOnly = true)
	public DmitriyItemEntity get(UUID userId, UUID itemId) {
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		return itemRepository.findByIdAndProfile_Id(itemId, profile.getId())
				.orElseThrow(() -> new DmitriyNotFoundException("Item not found"));
	}

	@Transactional
	public DmitriyItemEntity update(UUID userId, UUID itemId, DmitriyUpdateItemRequest req) {
		DmitriyItemEntity item = get(userId, itemId);
		if (req.name() != null) {
			item.setName(req.name());
		}
		if (req.color() != null) {
			item.setColor(req.color());
		}
		if (req.type() != null) {
			item.setType(req.type());
		}
		DmitriyItemEntity saved = itemRepository.save(item);
		activityService.record(userId, DmitriyActivityType.ITEM_UPDATED, saved.getId(), saved.getName());
		return saved;
	}

	@Transactional
	public void delete(UUID userId, UUID itemId) {
		DmitriyItemEntity item = get(userId, itemId);
		UUID targetId = item.getId();
		String name = item.getName();
		itemRepository.delete(item);
		storageService.deleteObject(item.getImageObjectKey());
		activityService.record(userId, DmitriyActivityType.ITEM_DELETED, targetId, name);
	}

	@Transactional(readOnly = true)
	public Page<DmitriyItemEntity> list(UUID userId, DmitriyItemType type, String color, String nameLike, Pageable pageable) {
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);

		Specification<DmitriyItemEntity> spec = Specification.where(DmitriyItemSpecifications.profileId(profile.getId()));
		if (type != null) {
			spec = spec.and(DmitriyItemSpecifications.type(type));
		}
		if (color != null && !color.isBlank()) {
			spec = spec.and(DmitriyItemSpecifications.colorEqualsIgnoreCase(color.trim()));
		}
		if (nameLike != null && !nameLike.isBlank()) {
			spec = spec.and(DmitriyItemSpecifications.nameLikeIgnoreCase(nameLike.trim()));
		}
		return itemRepository.findAll(spec, pageable);
	}
}

