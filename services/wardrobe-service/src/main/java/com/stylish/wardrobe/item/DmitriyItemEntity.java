package com.stylish.wardrobe.item;

import java.time.Instant;
import java.util.UUID;

import com.stylish.wardrobe.profile.DmitriyProfileEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class DmitriyItemEntity {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false, updatable = false)
	private DmitriyProfileEntity profile;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 40)
	private String color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DmitriyItemType type;

	@Column(name = "image_object_key", nullable = false, length = 512)
	private String imageObjectKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DmitriyItemEntity() {
	}

	public DmitriyItemEntity(DmitriyProfileEntity profile, String name, String color, DmitriyItemType type, String imageObjectKey) {
		this.profile = profile;
		this.name = name;
		this.color = color;
		this.type = type;
		this.imageObjectKey = imageObjectKey;
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public DmitriyProfileEntity getProfile() {
		return profile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public DmitriyItemType getType() {
		return type;
	}

	public void setType(DmitriyItemType type) {
		this.type = type;
	}

	public String getImageObjectKey() {
		return imageObjectKey;
	}

	public void setImageObjectKey(String imageObjectKey) {
		this.imageObjectKey = imageObjectKey;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

