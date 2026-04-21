package com.stylish.auth.event;

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
class AuthEventServiceTest {

	@Mock AuthEventRepository repository;

	@InjectMocks AuthEventService service;

	@Test
	void record_persistsEvent() {
		UUID userId = UUID.randomUUID();

		service.record(userId, "a@b.c", AuthEventType.LOGIN_SUCCESS, "1.1.1.1", "ua");

		ArgumentCaptor<AuthEvent> captor = ArgumentCaptor.forClass(AuthEvent.class);
		verify(repository).save(captor.capture());
		AuthEvent saved = captor.getValue();
		assertThat(saved.getUserId()).isEqualTo(userId);
		assertThat(saved.getEmail()).isEqualTo("a@b.c");
		assertThat(saved.getType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
		assertThat(saved.getIpAddress()).isEqualTo("1.1.1.1");
		assertThat(saved.getUserAgent()).isEqualTo("ua");
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void record_swallowsRepositoryFailure() {
		when(repository.save(any())).thenThrow(new RuntimeException("mongo down"));

		service.record(UUID.randomUUID(), "a@b.c", AuthEventType.REGISTER, null, null);
	}

	@Test
	void listForUser_delegatesWithClampedLimit() {
		UUID userId = UUID.randomUUID();
		when(repository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
				.thenReturn(List.of(new AuthEvent(userId, "a@b.c", AuthEventType.LOGIN_SUCCESS, null, null)));

		List<AuthEvent> result = service.listForUser(userId, 5_000);

		assertThat(result).hasSize(1);
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByUserIdOrderByCreatedAtDesc(eq(userId), captor.capture());
		assertThat(captor.getValue().getPageSize()).isEqualTo(200);
	}
}
