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

import com.stylish.wardrobe.api.BadRequestException;
import com.stylish.wardrobe.api.NotFoundException;
import com.stylish.wardrobe.generation.GenerationClient;
import com.stylish.wardrobe.item.ItemEntity;
import com.stylish.wardrobe.item.ItemRepository;
import com.stylish.wardrobe.item.ItemType;
import com.stylish.wardrobe.look.dto.GenerateLookRequest;
import com.stylish.wardrobe.look.dto.LookResponse;
import com.stylish.wardrobe.look.dto.UpdateLookRequest;
import com.stylish.wardrobe.photo.UserPhotoEntity;
import com.stylish.wardrobe.photo.UserPhotoRepository;
import com.stylish.wardrobe.profile.ProfileEntity;
import com.stylish.wardrobe.profile.ProfileService;
import com.stylish.wardrobe.storage.ObjectKeyFactory;
import com.stylish.wardrobe.storage.StorageService;

@Service
public class LookService {
	private final ProfileService profileService;
	private final UserPhotoRepository userPhotoRepository;
	private final ItemRepository itemRepository;
	private final LookRepository lookRepository;
	private final GenerationClient generationClient;
	private final StorageService storageService;
	private final ObjectKeyFactory objectKeyFactory;

	public LookService(
			ProfileService profileService,
			UserPhotoRepository userPhotoRepository,
			ItemRepository itemRepository,
			LookRepository lookRepository,
			GenerationClient generationClient,
			StorageService storageService,
			ObjectKeyFactory objectKeyFactory
	) {
		this.profileService = profileService;
		this.userPhotoRepository = userPhotoRepository;
		this.itemRepository = itemRepository;
		this.lookRepository = lookRepository;
		this.generationClient = generationClient;
		this.storageService = storageService;
		this.objectKeyFactory = objectKeyFactory;
	}

	@Transactional
	public LookResponse generate(UUID userId, GenerateLookRequest req) {
		Instant now = Instant.now();
		ProfileEntity profile = profileService.getProfileByUserId(userId);

		UserPhotoEntity photo = userPhotoRepository.findByIdAndProfile_Id(req.userPhotoId(), profile.getId())
				.orElseThrow(() -> new NotFoundException("User photo not found"));

		List<UUID> distinctItemIds = req.itemIds().stream().distinct().toList();
		List<ItemEntity> items = itemRepository.findAllByIdInAndProfile_Id(distinctItemIds, profile.getId());
		if (items.size() != distinctItemIds.size()) {
			throw new BadRequestException("Some items were not found");
		}

		Map<ItemType, Long> countsByType = items.stream()
				.collect(Collectors.groupingBy(ItemEntity::getType, Collectors.counting()));
		List<ItemType> dupTypes = countsByType.entrySet().stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.toList();
		if (!dupTypes.isEmpty()) {
			throw new BadRequestException("Only one item per type is allowed. Duplicates: " + dupTypes);
		}

		List<String> itemKeys = items.stream().map(ItemEntity::getImageObjectKey).toList();
		byte[] png = generationClient.generateLook(photo.getImageObjectKey(), itemKeys);
		if (png == null || png.length == 0) {
			throw new BadRequestException("Generation failed");
		}

		UUID lookId = UUID.randomUUID();
		String resultKey = objectKeyFactory.lookResultKey(profile.getId(), lookId);
		storageService.putObject(resultKey, "image/png", new ByteArrayInputStream(png), png.length);

		LookEntity look = new LookEntity(lookId, profile, photo, resultKey, req.name());
		look.setItems(new HashSet<>(items));
		LookEntity saved = lookRepository.save(look);

		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public LookResponse get(UUID userId, UUID lookId) {
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		LookEntity look = lookRepository.findByIdAndProfile_Id(lookId, profile.getId())
				.orElseThrow(() -> new NotFoundException("Look not found"));
		return toResponse(look);
	}

	@Transactional
	public LookResponse rename(UUID userId, UUID lookId, UpdateLookRequest req) {
		ProfileEntity profile = profileService.getProfileByUserId(userId);
		LookEntity look = lookRepository.findByIdAndProfile_Id(lookId, profile.getId())
				.orElseThrow(() -> new NotFoundException("Look not found"));
		look.setName(req.name());
		return toResponse(lookRepository.save(look));
	}

	private LookResponse toResponse(LookEntity look) {
		List<UUID> itemIds = look.getItems().stream().map(ItemEntity::getId).toList();
		String url = "/files/" + look.getResultImageObjectKey();
		return new LookResponse(
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

