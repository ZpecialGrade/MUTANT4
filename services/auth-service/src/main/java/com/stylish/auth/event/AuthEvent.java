package com.stylish.auth.event;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auth_events")
public class AuthEvent {

	@Id
	private String id;

	@Indexed
	private UUID userId;

	@Indexed
	private String email;

	private AuthEventType type;

	private String ipAddress;

	private String userAgent;

	@Indexed
	private Instant createdAt;

	public AuthEvent() {}

	public AuthEvent(UUID userId, String email, AuthEventType type, String ipAddress, String userAgent) {
		this.userId = userId;
		this.email = email;
		this.type = type;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
		this.createdAt = Instant.now();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public UUID getUserId() { return userId; }
	public void setUserId(UUID userId) { this.userId = userId; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public AuthEventType getType() { return type; }
	public void setType(AuthEventType type) { this.type = type; }

	public String getIpAddress() { return ipAddress; }
	public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
