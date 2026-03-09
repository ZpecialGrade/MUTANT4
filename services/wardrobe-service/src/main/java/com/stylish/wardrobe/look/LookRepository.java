package com.stylish.wardrobe.look;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LookRepository extends JpaRepository<LookEntity, UUID> {
	Optional<LookEntity> findByIdAndProfile_Id(UUID id, UUID profileId);
}

