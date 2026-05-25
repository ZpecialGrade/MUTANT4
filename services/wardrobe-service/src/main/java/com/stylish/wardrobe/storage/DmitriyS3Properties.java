package com.stylish.wardrobe.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record DmitriyS3Properties(
		String endpoint,
		String accessKey,
		String secretKey,
		String region,
		String bucket
) {
}

