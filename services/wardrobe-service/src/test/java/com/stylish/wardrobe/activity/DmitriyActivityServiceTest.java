package com.stylish.wardrobe.activity;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmitriyActivityServiceTest {

	@Mock DmitriyActivityRepository repository;

	@InjectMocks DmitriyActivityService service;

	@Test
	void record_persistsEvent() {
		UUID userId = UUID.randomUUID();
		UUID target = UUID.randomUUID();

		service.record(userId, DmitriyActivityType.ITEM_CREATED, target, "jeans");

		ArgumentCaptor<DmitriyActivityEvent> captor = ArgumentCaptor.forClass(DmitriyActivityEvent.class);
		verify(repository).save(captor.capture());
		DmitriyActivityEvent saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo(userId);
		assertThat(saved.getType()).isEqualTo(DmitriyActivityType.ITEM_CREATED);
		assertThat(saved.getTargetId()).isEqualTo(target);
		assertThat(saved.getDescription()).isEqualTo("jeans");
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void record_swallowsRepositoryFailure() {
		when(repository.save(any())).thenThrow(new RuntimeException("mongo down"));

		service.record(UUID.randomUUID(), DmitriyActivityType.ITEM_DELETED, null, null);
	}

	@Test
	void listForUser_clampsLimit() {
		UUID userId = UUID.randomUUID();
		when(repository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
				.thenReturn(List.of());

		service.listForUser(userId, 10_000);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByUserIdOrderByCreatedAtDesc(eq(userId), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(200);
	}
}
