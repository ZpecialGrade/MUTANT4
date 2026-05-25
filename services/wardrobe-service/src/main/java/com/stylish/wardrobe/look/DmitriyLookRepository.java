package com.stylish.wardrobe.look;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmitriyLookRepository extends JpaRepository<DmitriyLookEntity, UUID> {
	Optional<DmitriyLookEntity> findByIdAndProfile_Id(UUID id, UUID profileId);
}

