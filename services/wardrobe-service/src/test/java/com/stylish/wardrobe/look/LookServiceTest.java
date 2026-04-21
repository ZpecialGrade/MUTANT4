package com.stylish.wardrobe.look;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stylish.wardrobe.activity.ActivityService;
import com.stylish.wardrobe.activity.ActivityType;
import com.stylish.wardrobe.api.BadRequestException;
import com.stylish.wardrobe.generation.GenerationClient;
import com.stylish.wardrobe.item.ItemEntity;
import com.stylish.wardrobe.item.ItemRepository;
import com.stylish.wardrobe.item.ItemType;
import com.stylish.wardrobe.look.dto.GenerateLookRequest;
import com.stylish.wardrobe.look.dto.LookResponse;
import com.stylish.wardrobe.photo.UserPhotoEntity;
import com.stylish.wardrobe.photo.UserPhotoRepository;
import com.stylish.wardrobe.profile.ProfileEntity;
import com.stylish.wardrobe.profile.ProfileService;
import com.stylish.wardrobe.storage.ObjectKeyFactory;
import com.stylish.wardrobe.storage.StorageService;

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
class LookServiceTest {

	@Mock ProfileService profileService;
	@Mock UserPhotoRepository userPhotoRepository;
	@Mock ItemRepository itemRepository;
	@Mock LookRepository lookRepository;
	@Mock GenerationClient generationClient;
	@Mock StorageService storageService;
	@Mock ObjectKeyFactory objectKeyFactory;
	@Mock ActivityService activityService;

	@InjectMocks LookService lookService;

	private ProfileEntity profile(UUID userId) {
		ProfileEntity p = new ProfileEntity(userId, "p");
		ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
		return p;
	}

	private ItemEntity item(ProfileEntity p, ItemType type) {
		ItemEntity e = new ItemEntity(p, "n", "c", type, "items/" + type);
		ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
		return e;
	}

	private UserPhotoEntity photo(ProfileEntity p) {
		UserPhotoEntity ph = new UserPhotoEntity(p, "user-photos/p");
		ReflectionTestUtils.setField(ph, "id", UUID.randomUUID());
		return ph;
	}

	@Test
	void generate_duplicateItemTypes_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		ProfileEntity p = profile(userId);
		UserPhotoEntity ph = photo(p);
		ItemEntity top1 = item(p, ItemType.TOP);
		ItemEntity top2 = item(p, ItemType.TOP);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top1, top2));

		GenerateLookRequest req = new GenerateLookRequest(ph.getId(), List.of(top1.getId(), top2.getId()), "name");

		assertThatThrownBy(() -> lookService.generate(userId, req))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Duplicates");
	}

	@Test
	void generate_missingItem_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		ProfileEntity p = profile(userId);
		UserPhotoEntity ph = photo(p);
		ItemEntity top = item(p, ItemType.TOP);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top));

		GenerateLookRequest req = new GenerateLookRequest(ph.getId(), List.of(top.getId(), UUID.randomUUID()), null);

		assertThatThrownBy(() -> lookService.generate(userId, req))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void generate_happyPath_savesLookAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		ProfileEntity p = profile(userId);
		UserPhotoEntity ph = photo(p);
		ItemEntity top = item(p, ItemType.TOP);
		ItemEntity bottom = item(p, ItemType.BOTTOM);

		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(userPhotoRepository.findByIdAndProfile_Id(ph.getId(), p.getId())).thenReturn(Optional.of(ph));
		when(itemRepository.findAllByIdInAndProfile_Id(anyList(), eq(p.getId()))).thenReturn(List.of(top, bottom));
		when(generationClient.generateLook(anyString(), anyList())).thenReturn(new byte[]{1, 2, 3, 4});
		when(objectKeyFactory.lookResultKey(eq(p.getId()), any(UUID.class))).thenReturn("looks/result.png");
		when(lookRepository.save(any(LookEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		GenerateLookRequest req = new GenerateLookRequest(ph.getId(), List.of(top.getId(), bottom.getId()), "outfit");
		LookResponse resp = lookService.generate(userId, req);

		assertThat(resp.name()).isEqualTo("outfit");
		verify(storageService).putObject(eq("looks/result.png"), eq("image/png"), any(), anyLong());
		verify(activityService).record(eq(userId), eq(ActivityType.LOOK_GENERATED), any(UUID.class), eq("outfit"));
	}
}
