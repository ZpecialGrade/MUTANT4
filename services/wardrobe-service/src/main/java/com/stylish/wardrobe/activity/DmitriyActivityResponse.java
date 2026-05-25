package com.stylish.wardrobe.activity;

import java.time.Instant;
import java.util.UUID;

public record DmitriyActivityResponse(
		String id,
		UUID userId,
		DmitriyActivityType type,
		UUID targetId,
		String description,
		Instant createdAt
) {
	public static DmitriyActivityResponse of(DmitriyActivityEvent e) {
		return new DmitriyActivityResponse(
				e.getId(),
				e.getUserId(),
				e.getType(),
				e.getTargetId(),
				e.getDescription(),
				e.getCreatedAt()
		);
	}
}
