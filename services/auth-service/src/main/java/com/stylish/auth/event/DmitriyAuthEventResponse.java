package com.stylish.auth.event;

import java.time.Instant;
import java.util.UUID;

public record DmitriyAuthEventResponse(
		String id,
		UUID userId,
		String email,
		DmitriyAuthEventType type,
		String ipAddress,
		String userAgent,
		Instant createdAt
) {
	public static DmitriyAuthEventResponse of(DmitriyAuthEvent e) {
		return new DmitriyAuthEventResponse(
				e.getId(),
				e.getUserId(),
				e.getEmail(),
				e.getType(),
				e.getIpAddress(),
				e.getUserAgent(),
				e.getCreatedAt()
		);
	}
}
