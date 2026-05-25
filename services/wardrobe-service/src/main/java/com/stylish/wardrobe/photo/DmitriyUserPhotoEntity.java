package com.stylish.wardrobe.photo;

import java.time.Instant;
import java.util.UUID;

import com.stylish.wardrobe.profile.DmitriyProfileEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_photos")
public class DmitriyUserPhotoEntity {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false, updatable = false)
	private DmitriyProfileEntity profile;

	@Column(name = "image_object_key", nullable = false, length = 512)
	private String imageObjectKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected DmitriyUserPhotoEntity() {
	}

	public DmitriyUserPhotoEntity(DmitriyProfileEntity profile, String imageObjectKey) {
		this.profile = profile;
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

	public String getImageObjectKey() {
		return imageObjectKey;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

