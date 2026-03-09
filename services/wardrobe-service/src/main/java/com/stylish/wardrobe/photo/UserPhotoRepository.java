package com.stylish.wardrobe.photo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPhotoRepository extends JpaRepository<UserPhotoEntity, UUID> {
	Optional<UserPhotoEntity> findByIdAndProfile_Id(UUID id, UUID profileId);
}

