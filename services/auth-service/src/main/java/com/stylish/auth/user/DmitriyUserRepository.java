package com.stylish.auth.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DmitriyUserRepository extends JpaRepository<DmitriyUserEntity, UUID> {
	boolean existsByEmailIgnoreCase(String email);

	Optional<DmitriyUserEntity> findByEmailIgnoreCase(String email);
}

