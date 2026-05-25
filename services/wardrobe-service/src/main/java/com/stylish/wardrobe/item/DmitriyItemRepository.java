package com.stylish.wardrobe.item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DmitriyItemRepository extends JpaRepository<DmitriyItemEntity, UUID>, JpaSpecificationExecutor<DmitriyItemEntity> {
	Optional<DmitriyItemEntity> findByIdAndProfile_Id(UUID id, UUID profileId);

	List<DmitriyItemEntity> findAllByIdInAndProfile_Id(Collection<UUID> ids, UUID profileId);
}

