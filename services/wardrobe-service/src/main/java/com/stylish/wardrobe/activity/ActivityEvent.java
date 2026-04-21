package com.stylish.wardrobe.activity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "activity_events")
public class ActivityEvent {

	@Id
	private String id;

	@Indexed
	private UUID userId;

	private ActivityType type;

	private UUID targetId;

	private String description;

	@Indexed
	private Instant createdAt;

	public ActivityEvent() {}

	public ActivityEvent(UUID userId, ActivityType type, UUID targetId, String description) {
		this.userId = userId;
		this.type = type;
		this.targetId = targetId;
		this.description = description;
		this.createdAt = Instant.now();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public UUID getUserId() { return userId; }
	public void setUserId(UUID userId) { this.userId = userId; }

	public ActivityType getType() { return type; }
	public void setType(ActivityType type) { this.type = type; }

	public UUID getTargetId() { return targetId; }
	public void setTargetId(UUID targetId) { this.targetId = targetId; }

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
