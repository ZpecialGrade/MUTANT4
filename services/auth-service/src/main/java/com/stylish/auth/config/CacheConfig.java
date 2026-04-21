package com.stylish.auth.config;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	public static final String USERS_BY_EMAIL = "usersByEmail";

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager mgr = new CaffeineCacheManager(USERS_BY_EMAIL);
		mgr.setCaffeine(Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(Duration.ofMinutes(5)));
		return mgr;
	}
}
