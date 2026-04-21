package com.stylish.wardrobe.config;

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

	public static final String PROFILES_BY_USER_ID = "profilesByUserId";

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager mgr = new CaffeineCacheManager(PROFILES_BY_USER_ID);
		mgr.setCaffeine(Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(Duration.ofMinutes(10)));
		return mgr;
	}
}
