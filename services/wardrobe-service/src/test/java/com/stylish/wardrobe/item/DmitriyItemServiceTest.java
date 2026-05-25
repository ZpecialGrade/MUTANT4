package com.stylish.wardrobe.item;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import com.stylish.wardrobe.activity.DmitriyActivityService;
import com.stylish.wardrobe.activity.DmitriyActivityType;
import com.stylish.wardrobe.api.DmitriyBadRequestException;
import com.stylish.wardrobe.api.DmitriyNotFoundException;
import com.stylish.wardrobe.item.dto.DmitriyCreateItemMetadata;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DmitriyItemServiceTest {

	@Mock DmitriyProfileService profileService;
	@Mock DmitriyItemRepository itemRepository;
	@Mock DmitriyStorageService storageService;
	@Mock DmitriyObjectKeyFactory objectKeyFactory;
	@Mock DmitriyActivityService activityService;

	@InjectMocks DmitriyItemService itemService;

	private DmitriyProfileEntity profile(UUID userId) {
		DmitriyProfileEntity p = new DmitriyProfileEntity(userId, "Alice");
		ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
		return p;
	}

	private DmitriyItemEntity savedItem(DmitriyProfileEntity p, String name) {
		DmitriyItemEntity e = new DmitriyItemEntity(p, name, "red", DmitriyItemType.TOP, "items/x/y");
		ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
		return e;
	}

	@Test
	void create_happyPath_uploadsToStorageSavesAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity profile = profile(userId);
		when(profileService.getProfileByUserId(userId)).thenReturn(profile);
		when(objectKeyFactory.itemImageKey(eq(profile.getId()), anyString())).thenReturn("items/key");
		when(itemRepository.save(any(DmitriyItemEntity.class))).thenAnswer(inv -> {
			DmitriyItemEntity arg = inv.getArgument(0);
			ReflectionTestUtils.setField(arg, "id", UUID.randomUUID());
			return arg;
		});
		DmitriyCreateItemMetadata meta = new DmitriyCreateItemMetadata("jeans", "blue", DmitriyItemType.BOTTOM);
		MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1, 2, 3});

		DmitriyItemEntity result = itemService.create(userId, meta, file);

		assertThat(result.getName()).isEqualTo("jeans");
		verify(storageService).putObject(eq("items/key"), eq("image/png"), any(ByteArrayInputStream.class), anyLong());
		verify(itemRepository).save(any(DmitriyItemEntity.class));
		verify(activityService).record(eq(userId), eq(DmitriyActivityType.ITEM_CREATED), eq(result.getId()), eq("jeans"));
	}

	@Test
	void create_emptyFile_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		DmitriyCreateItemMetadata meta = new DmitriyCreateItemMetadata("jeans", "blue", DmitriyItemType.BOTTOM);
		MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

		assertThatThrownBy(() -> itemService.create(userId, meta, empty))
				.isInstanceOf(DmitriyBadRequestException.class);
	}

	@Test
	void get_ownedByOtherUser_throwsNotFound() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity p = profile(userId);
		UUID itemId = UUID.randomUUID();
		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(itemRepository.findByIdAndProfile_Id(itemId, p.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> itemService.get(userId, itemId))
				.isInstanceOf(DmitriyNotFoundException.class);
	}

	@Test
	void delete_removesFromDbStorageAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		DmitriyProfileEntity p = profile(userId);
		DmitriyItemEntity item = savedItem(p, "shoes");
		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(itemRepository.findByIdAndProfile_Id(item.getId(), p.getId())).thenReturn(Optional.of(item));

		itemService.delete(userId, item.getId());

		verify(itemRepository).delete(item);
		verify(storageService).deleteObject("items/x/y");
		verify(activityService).record(eq(userId), eq(DmitriyActivityType.ITEM_DELETED), eq(item.getId()), eq("shoes"));
	}
}
