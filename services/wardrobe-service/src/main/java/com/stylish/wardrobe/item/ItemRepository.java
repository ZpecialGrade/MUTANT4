package com.stylish.wardrobe.item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository extends JpaRepository<ItemEntity, UUID>, JpaSpecificationExecutor<ItemEntity> {
	Optional<ItemEntity> findByIdAndProfile_Id(UUID id, UUID profileId);

	List<ItemEntity> findAllByIdInAndProfile_Id(Collection<UUID> ids, UUID profileId);
}

