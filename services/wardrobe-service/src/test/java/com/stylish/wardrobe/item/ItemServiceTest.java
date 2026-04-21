package com.stylish.wardrobe.item;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import com.stylish.wardrobe.activity.ActivityService;
import com.stylish.wardrobe.activity.ActivityType;
import com.stylish.wardrobe.api.BadRequestException;
import com.stylish.wardrobe.api.NotFoundException;
import com.stylish.wardrobe.item.dto.CreateItemMetadata;
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
class ItemServiceTest {

	@Mock ProfileService profileService;
	@Mock ItemRepository itemRepository;
	@Mock StorageService storageService;
	@Mock ObjectKeyFactory objectKeyFactory;
	@Mock ActivityService activityService;

	@InjectMocks ItemService itemService;

	private ProfileEntity profile(UUID userId) {
		ProfileEntity p = new ProfileEntity(userId, "Alice");
		ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
		return p;
	}

	private ItemEntity savedItem(ProfileEntity p, String name) {
		ItemEntity e = new ItemEntity(p, name, "red", ItemType.TOP, "items/x/y");
		ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
		return e;
	}

	@Test
	void create_happyPath_uploadsToStorageSavesAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		ProfileEntity profile = profile(userId);
		when(profileService.getProfileByUserId(userId)).thenReturn(profile);
		when(objectKeyFactory.itemImageKey(eq(profile.getId()), anyString())).thenReturn("items/key");
		when(itemRepository.save(any(ItemEntity.class))).thenAnswer(inv -> {
			ItemEntity arg = inv.getArgument(0);
			ReflectionTestUtils.setField(arg, "id", UUID.randomUUID());
			return arg;
		});
		CreateItemMetadata meta = new CreateItemMetadata("jeans", "blue", ItemType.BOTTOM);
		MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1, 2, 3});

		ItemEntity result = itemService.create(userId, meta, file);

		assertThat(result.getName()).isEqualTo("jeans");
		verify(storageService).putObject(eq("items/key"), eq("image/png"), any(ByteArrayInputStream.class), anyLong());
		verify(itemRepository).save(any(ItemEntity.class));
		verify(activityService).record(eq(userId), eq(ActivityType.ITEM_CREATED), eq(result.getId()), eq("jeans"));
	}

	@Test
	void create_emptyFile_throwsBadRequest() {
		UUID userId = UUID.randomUUID();
		CreateItemMetadata meta = new CreateItemMetadata("jeans", "blue", ItemType.BOTTOM);
		MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

		assertThatThrownBy(() -> itemService.create(userId, meta, empty))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void get_ownedByOtherUser_throwsNotFound() {
		UUID userId = UUID.randomUUID();
		ProfileEntity p = profile(userId);
		UUID itemId = UUID.randomUUID();
		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(itemRepository.findByIdAndProfile_Id(itemId, p.getId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> itemService.get(userId, itemId))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void delete_removesFromDbStorageAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		ProfileEntity p = profile(userId);
		ItemEntity item = savedItem(p, "shoes");
		when(profileService.getProfileByUserId(userId)).thenReturn(p);
		when(itemRepository.findByIdAndProfile_Id(item.getId(), p.getId())).thenReturn(Optional.of(item));

		itemService.delete(userId, item.getId());

		verify(itemRepository).delete(item);
		verify(storageService).deleteObject("items/x/y");
		verify(activityService).record(eq(userId), eq(ActivityType.ITEM_DELETED), eq(item.getId()), eq("shoes"));
	}
}
