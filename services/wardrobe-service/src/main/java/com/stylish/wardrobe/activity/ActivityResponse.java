package com.stylish.wardrobe.activity;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
		String id,
		UUID userId,
		ActivityType type,
		UUID targetId,
		String description,
		Instant createdAt
) {
	public static ActivityResponse of(ActivityEvent e) {
		return new ActivityResponse(
				e.getId(),
				e.getUserId(),
				e.getType(),
				e.getTargetId(),
				e.getDescription(),
				e.getCreatedAt()
		);
	}
}
