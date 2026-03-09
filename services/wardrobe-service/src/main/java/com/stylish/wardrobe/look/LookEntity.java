package com.stylish.wardrobe.look;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.stylish.wardrobe.item.ItemEntity;
import com.stylish.wardrobe.photo.UserPhotoEntity;
import com.stylish.wardrobe.profile.ProfileEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "looks")
public class LookEntity {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profile_id", nullable = false, updatable = false)
	private ProfileEntity profile;

	@Column(length = 120)
	private String name;

	@Column(name = "result_image_object_key", nullable = false, length = 512)
	private String resultImageObjectKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_user_photo_id", nullable = false, updatable = false)
	private UserPhotoEntity sourceUserPhoto;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "look_items",
			joinColumns = @JoinColumn(name = "look_id"),
			inverseJoinColumns = @JoinColumn(name = "item_id")
	)
	private Set<ItemEntity> items = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected LookEntity() {
	}

	public LookEntity(UUID id, ProfileEntity profile, UserPhotoEntity sourceUserPhoto, String resultImageObjectKey, String name) {
		this.id = id;
		this.profile = profile;
		this.sourceUserPhoto = sourceUserPhoto;
		this.resultImageObjectKey = resultImageObjectKey;
		this.name = name;
	}

	@PrePersist
	void prePersist() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (items == null) {
			items = new HashSet<>();
		}
	}

	public UUID getId() {
		return id;
	}

	public ProfileEntity getProfile() {
		return profile;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getResultImageObjectKey() {
		return resultImageObjectKey;
	}

	public void setResultImageObjectKey(String resultImageObjectKey) {
		this.resultImageObjectKey = resultImageObjectKey;
	}

	public UserPhotoEntity getSourceUserPhoto() {
		return sourceUserPhoto;
	}

	public Set<ItemEntity> getItems() {
		return items;
	}

	public void setItems(Set<ItemEntity> items) {
		this.items = items;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

