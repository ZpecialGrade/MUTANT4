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
public class ActivityService {

	private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

	private final ActivityRepository repository;

	public ActivityService(ActivityRepository repository) {
		this.repository = repository;
	}

	@Async("activityExecutor")
	public void record(UUID userId, ActivityType type, UUID targetId, String description) {
		try {
			repository.save(new ActivityEvent(userId, type, targetId, description));
		} catch (Exception e) {
			log.warn("failed to write activity event type={} user={}: {}", type, userId, e.getMessage());
		}
	}

	public List<ActivityEvent> listForUser(UUID userId, int limit) {
		Pageable page = PageRequest.of(0, Math.min(Math.max(limit, 1), 200));
		return repository.findByUserIdOrderByCreatedAtDesc(userId, page);
	}
}
