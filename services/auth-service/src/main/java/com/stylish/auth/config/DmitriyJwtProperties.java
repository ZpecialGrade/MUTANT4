package com.stylish.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record DmitriyJwtProperties(
		String accessSecret,
		Duration accessTtl,
		Duration refreshTtl
) {
}

