package com.stylish.auth.event;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DmitriyAuthEventRepository extends MongoRepository<DmitriyAuthEvent, String> {

	List<DmitriyAuthEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
