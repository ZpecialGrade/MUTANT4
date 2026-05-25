package com.stylish.wardrobe.look;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyBadRequestException;
import com.stylish.wardrobe.generation.DmitriyGenerationClient;
import com.stylish.wardrobe.item.DmitriyItemEntity;
import com.stylish.wardrobe.item.DmitriyItemRepository;
import com.stylish.wardrobe.item.DmitriyItemType;
import com.stylish.wardrobe.look.dto.DmitriyGenerateLookRequest;
import com.stylish.wardrobe.look.dto.DmitriyLookResponse;
import com.stylish.wardrobe.photo.DmitriyUserPhotoEntity;
import com.stylish.wardrobe.photo.DmitriyUserPhotoRepository;
import com.stylish.wardrobe.profile.DmitriyProfileEntity;
import com.stylish.wardrobe.profile.DmitriyProfileService;
import com.stylish.wardrobe.storage.DmitriyObjectKeyFactory;
import com.stylish.wardrobe.storage.DmitriyStorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DmitriyLookServiceTest {

	@Mock DmitriyProfileService profileService;
	@Mock DmitriyUserPhotoRepository userPhotoRepository;
	@Mock DmitriyItemRepository itemRepository;
	@Mock DmitriyLookRepository lookRepository;
	@Mock DmitriyGenerationClient generationClient;
	@Mock DmitriyStorageService storageService;
	@Mock DmitriyObjectKeyFactory objectKeyFactory;
	@Mock DmitriyActivityService activityService;

	@InjectMocks DmitriyLookService lookService;

	private DmitriyProfileEntity profile(UUID userId) {
		DmitriyProfileEntity p = new DmitriyProfileEntity(userId, "p");
		ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
		return p;
	}

	private DmitriyItemEntity item(DmitriyProfileEntity p, DmitriyItemType type) {
		DmitriyItemEntity e = new DmitriyItemEntity(p, "n", "c", type, "items/" + type);
		ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
		return e;
	}

	private DmitriyUserPhotoEntity photo(DmitriyProfileEntity p) {
		DmitriyUserPhotoEntity ph = new DmitriyUserPhotoEntity(p, "user-photos/p");
		ReflectionTestUtils.setField(ph, "id", UUID.randomUUID());
		return ph;
	}

	@Test
	void generate_duplicateItemTypes_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity p = profile(userId);
		DmitriyUserPhotoEntity ph = photo(p);
		DmitriyItemEntity top1 = item(p, DmitriyItemType.TOP);
		DmitriyItemEntity top2 = item(p, DmitriyItemType.TOP);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top1, top2));

		DmitriyGenerateLookRequest req = new DmitriyGenerateLookRequest(ph.getId(), List.of(top1.getId(), top2.getId()), "name");

		assertThatThrownBy(() -> lookService.generate(userId, req))
				.isInstanceOf(DmitriyBadRequestException.class)
				.hasMessageContaining("Duplicates");
	}

	@Test
	void generate_missingItem_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity p = profile(userId);
		DmitriyUserPhotoEntity ph = photo(p);
		DmitriyItemEntity top = item(p, DmitriyItemType.TOP);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top));

		DmitriyGenerateLookRequest req = new DmitriyGenerateLookRequest(ph.getId(), List.of(top.getId(), UUID.randomUUID()), null);

		assertThatThrownBy(() -> lookService.generate(userId, req))
				.isInstanceOf(DmitriyBadRequestException.class);
	}

	@Test
	void generate_happyPath_savesLookAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity p = profile(userId);
		DmitriyUserPhotoEntity ph = photo(p);
		DmitriyItemEntity top = item(p, DmitriyItemType.TOP);
		DmitriyItemEntity bottom = item(p, DmitriyItemType.BOTTOM);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top, bottom));
		when(generationClient.generateLook(anyString(), anyList())).thenReturn(new byte[]{1, 2, 3, 4});
		when(objectKeyFactory.lookResultKey(eq(p.getId()), any(UUID.class))).thenReturn("looks/result.png");
		when(lookRepository.save(any(DmitriyLookEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		DmitriyGenerateLookRequest req = new DmitriyGenerateLookRequest(ph.getId(), List.of(top.getId(), bottom.getId()), "outfit");
		DmitriyLookResponse resp = lookService.generate(userId, req);

		assertThat(resp.name()).isEqualTo("outfit");
		verify(storageService).putObject(eq("looks/result.png"), eq("image/png"), any(), anyLong());
		verify(activityService).record(eq(userId), eq(DmitriyActivityType.LOOK_GENERATED), any(UUID.class), eq("outfit"));
	}
}
