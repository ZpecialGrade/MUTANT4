package com.stylish.auth.token;

import java.time.Instant;
import java.util.UUID;

import com.stylish.auth.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private UserEntity user;

	@Column(name = "secret_hash", nullable = false, length = 255)
	private String secretHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshTokenEntity() {
	}

	public RefreshTokenEntity(UUID id, UserEntity user, String secretHash, Instant expiresAt) {
		this.id = id;
		this.user = user;
		this.secretHash = secretHash;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public UserEntity getUser() {
		return user;
	}

	public String getSecretHash() {
		return secretHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now) || expiresAt.equals(now);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}
}

