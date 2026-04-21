package com.stylish.wardrobe.profile;

import java.util.Optional;
import java.util.UUID;

import com.stylish.wardrobe.activity.ActivityService;
import com.stylish.wardrobe.activity.ActivityType;
import com.stylish.wardrobe.api.ConflictException;
import com.stylish.wardrobe.api.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

	@Mock ProfileRepository profileRepository;
	@Mock ActivityService activityService;

	@InjectMocks ProfileService profileService;

	@Test
	void createProfile_happyPath_savesAndRecordsActivity() {
		UUID userId = UUID.randomUUID();
		when(profileRepository.existsByUserId(userId)).thenReturn(false);
		when(profileRepository.save(any(ProfileEntity.class))).thenAnswer(inv -> {
			ProfileEntity p = inv.getArgument(0);
			ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
			return p;
		});

		ProfileEntity saved = profileService.createProfile(userId, "Alice");

		assertThat(saved.getDisplayName()).isEqualTo("Alice");
		verify(activityService).record(eq(userId), eq(ActivityType.PROFILE_CREATED), eq(saved.getId()), eq("Alice"));
	}

	@Test
	void createProfile_duplicate_throwsConflict() {
		UUID userId = UUID.randomUUID();
		when(profileRepository.existsByUserId(userId)).thenReturn(true);

		assertThatThrownBy(() -> profileService.createProfile(userId, "Bob"))
				.isInstanceOf(ConflictException.class);

		verify(profileRepository, never()).save(any());
		verify(activityService, never()).record(any(), any(), any(), any());
	}

	@Test
	void getProfileByUserId_missing_throwsNotFound() {
		UUID userId = UUID.randomUUID();
		when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> profileService.getProfileByUserId(userId))
				.isInstanceOf(NotFoundException.class);
	}
}
