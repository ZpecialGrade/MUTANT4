package com.stylish.wardrobe.look;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyBadRequestException;
import com.stylish.wardrobe.api.DmitriyNotFoundException;
import com.stylish.wardrobe.generation.DmitriyGenerationClient;
import com.stylish.wardrobe.item.DmitriyItemEntity;
import com.stylish.wardrobe.item.DmitriyItemRepository;
import com.stylish.wardrobe.item.DmitriyItemType;
import com.stylish.wardrobe.look.dto.DmitriyGenerateLookRequest;
import com.stylish.wardrobe.look.dto.DmitriyLookResponse;
import com.stylish.wardrobe.look.dto.DmitriyUpdateLookRequest;
import com.stylish.wardrobe.photo.DmitriyUserPhotoEntity;
import com.stylish.wardrobe.photo.DmitriyUserPhotoRepository;
import com.stylish.wardrobe.profile.DmitriyProfileEntity;
import com.stylish.wardrobe.profile.DmitriyProfileService;
import com.stylish.wardrobe.storage.DmitriyObjectKeyFactory;
import com.stylish.wardrobe.storage.DmitriyStorageService;

@Service
public class DmitriyLookService {
	private final DmitriyProfileService profileService;
	private final DmitriyUserPhotoRepository userPhotoRepository;
	private final DmitriyItemRepository itemRepository;
	private final DmitriyLookRepository lookRepository;
	private final DmitriyGenerationClient generationClient;
	private final DmitriyStorageService storageService;
	private final DmitriyObjectKeyFactory objectKeyFactory;
	private final DmitriyActivityService activityService;

	public DmitriyLookService(
			DmitriyProfileService profileService,
			DmitriyUserPhotoRepository userPhotoRepository,
			DmitriyItemRepository itemRepository,
			DmitriyLookRepository lookRepository,
			DmitriyGenerationClient generationClient,
			DmitriyStorageService storageService,
			DmitriyObjectKeyFactory objectKeyFactory,
			DmitriyActivityService activityService
	) {
		this.profileService = profileService;
		this.userPhotoRepository = userPhotoRepository;
		this.itemRepository = itemRepository;
		this.lookRepository = lookRepository;
		this.generationClient = generationClient;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
		this.activityService = activityService;
	}

	@Transactional
	public DmitriyLookResponse generate(UUID userId, DmitriyGenerateLookRequest req) {
		Instant now = Instant.now();
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);

		DmitriyUserPhotoEntity photo = userPhotoRepository.findByIdAndProfile_Id(req.userPhotoId(), profile.getId())
				.orElseThrow(() -> new DmitriyNotFoundException("User photo not found"));

		List<UUID> distinctItemIds = req.itemIds().stream().distinct().toList();
		List<DmitriyItemEntity> items = itemRepository.findAllByIdInAndProfile_Id(distinctItemIds, profile.getId());
		if (items.size() != distinctItemIds.size()) {
			throw new DmitriyBadRequestException("Some items were not found");
		}

		Map<DmitriyItemType, Long> countsByType = items.stream()
				.collect(Collectors.groupingBy(DmitriyItemEntity::getType, Collectors.counting()));
		List<DmitriyItemType> dupTypes = countsByType.entrySet().stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.toList();
		if (!dupTypes.isEmpty()) {
			throw new DmitriyBadRequestException("Only one item per type is allowed. Duplicates: " + dupTypes);
		}

		List<String> itemKeys = items.stream().map(DmitriyItemEntity::getImageObjectKey).toList();
		byte[] png = generationClient.generateLook(photo.getImageObjectKey(), itemKeys);
		if (png == null || png.length == 0) {
			throw new DmitriyBadRequestException("Generation failed");
		}

		UUID lookId = UUID.randomUUID();
		String resultKey = objectKeyFactory.lookResultKey(profile.getId(), lookId);
		storageService.putObject(resultKey, "image/png", new ByteArrayInputStream(png), png.length);

		DmitriyLookEntity look = new DmitriyLookEntity(lookId, profile, photo, resultKey, req.name());
		look.setItems(new HashSet<>(items));
		DmitriyLookEntity saved = lookRepository.save(look);

		activityService.record(userId, DmitriyActivityType.LOOK_GENERATED, saved.getId(), saved.getName());
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public DmitriyLookResponse get(UUID userId, UUID lookId) {
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		DmitriyLookEntity look = lookRepository.findByIdAndProfile_Id(lookId, profile.getId())
				.orElseThrow(() -> new DmitriyNotFoundException("Look not found"));
		return toResponse(look);
	}

	@Transactional
	public DmitriyLookResponse rename(UUID userId, UUID lookId, DmitriyUpdateLookRequest req) {
		DmitriyProfileEntity profile = profileService.getProfileByUserId(userId);
		DmitriyLookEntity look = lookRepository.findByIdAndProfile_Id(lookId, profile.getId())
				.orElseThrow(() -> new DmitriyNotFoundException("Look not found"));
		look.setName(req.name());
		DmitriyLookEntity saved = lookRepository.save(look);
		activityService.record(userId, DmitriyActivityType.LOOK_RENAMED, saved.getId(), saved.getName());
		return toResponse(saved);
	}

	private DmitriyLookResponse toResponse(DmitriyLookEntity look) {
		List<UUID> itemIds = look.getItems().stream().map(DmitriyItemEntity::getId).toList();
		String url = "/files/" + look.getResultImageObjectKey();
		return new DmitriyLookResponse(
				look.getId(),
				look.getName(),
				look.getSourceUserPhoto().getId(),
				itemIds,
				look.getResultImageObjectKey(),
				url,
				look.getCreatedAt()
		);
	}
}

