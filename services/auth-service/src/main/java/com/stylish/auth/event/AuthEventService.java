package com.stylish.auth.event;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuthEventService {

	private static final Logger log = LoggerFactory.getLogger(AuthEventService.class);

	private final AuthEventRepository repository;

	public AuthEventService(AuthEventRepository repository) {
		this.repository = repository;
	}

	@Async("auditExecutor")
	public void record(UUID userId, String email, AuthEventType type, String ipAddress, String userAgent) {
		try {
			repository.save(new AuthEvent(userId, email, type, ipAddress, userAgent));
		} catch (Exception e) {
			log.warn("failed to write auth event type={} email={}: {}", type, email, e.getMessage());
		}
	}

	public List<AuthEvent> listForUser(UUID userId, int limit) {
		Pageable page = PageRequest.of(0, Math.min(Math.max(limit, 1), 200));
		return repository.findByUserIdOrderByCreatedAtDesc(userId, page);
	}
}
