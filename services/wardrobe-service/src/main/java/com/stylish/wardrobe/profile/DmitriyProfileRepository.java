package com.stylish.wardrobe.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmitriyProfileRepository extends JpaRepository<DmitriyProfileEntity, UUID> {
	Optional<DmitriyProfileEntity> findByUserId(UUID userId);

	boolean existsByUserId(UUID userId);
}

