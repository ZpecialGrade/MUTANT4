package com.stylish.auth.event;

import java.time.Instant;
import java.util.UUID;

public record AuthEventResponse(
		String id,
		UUID userId,
		String email,
		AuthEventType type,
		String ipAddress,
		String userAgent,
		Instant createdAt
) {
	public static AuthEventResponse of(AuthEvent e) {
		return new AuthEventResponse(
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
