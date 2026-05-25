package com.stylish.wardrobe.activity;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DmitriyActivityService {

	private static final Logger log = LoggerFactory.getLogger(DmitriyActivityService.class);

	private final DmitriyActivityRepository repository;

	public DmitriyActivityService(DmitriyActivityRepository repository) {
		this.repository = repository;
	}

	@Async("activityExecutor")
	public void record(UUID userId, DmitriyActivityType type, UUID targetId, String description) {
		try {
			repository.save(new DmitriyActivityEvent(userId, type, targetId, description));
		} catch (Exception e) {
			log.warn("failed to write activity event type={} user={}: {}", type, userId, e.getMessage());
		}
	}

	public List<DmitriyActivityEvent> listForUser(UUID userId, int limit) {
		Pageable page = PageRequest.of(0, Math.min(Math.max(limit, 1), 200));
		return repository.findByUserIdOrderByCreatedAtDesc(userId, page);
	}
}
