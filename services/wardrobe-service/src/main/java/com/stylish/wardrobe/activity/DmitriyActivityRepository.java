package com.stylish.wardrobe.activity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DmitriyActivityRepository extends MongoRepository<DmitriyActivityEvent, String> {

	List<DmitriyActivityEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
