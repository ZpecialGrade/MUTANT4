package com.stylish.auth.user;

import com.stylish.auth.config.DmitriyCacheConfig;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DmitriyUserLookupService {

	private final DmitriyUserRepository userRepository;

	public DmitriyUserLookupService(DmitriyUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = DmitriyCacheConfig.USERS_BY_EMAIL, key = "#email", unless = "#result == null")
	public DmitriyUserEntity findByEmailOrNull(String email) {
		return userRepository.findByEmailIgnoreCase(email).orElse(null);
	}

	@CacheEvict(cacheNames = DmitriyCacheConfig.USERS_BY_EMAIL, key = "#email")
	public void evictByEmail(String email) {
	}
}
