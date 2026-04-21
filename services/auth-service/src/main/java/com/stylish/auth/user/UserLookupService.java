package com.stylish.auth.user;

import com.stylish.auth.config.CacheConfig;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserLookupService {

	private final UserRepository userRepository;

	public UserLookupService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Cacheable(cacheNames = CacheConfig.USERS_BY_EMAIL, key = "#email", unless = "#result == null")
	public UserEntity findByEmailOrNull(String email) {
		return userRepository.findByEmailIgnoreCase(email).orElse(null);
	}

	@CacheEvict(cacheNames = CacheConfig.USERS_BY_EMAIL, key = "#email")
	public void evictByEmail(String email) {
	}
}
