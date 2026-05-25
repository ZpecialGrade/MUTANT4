package com.stylish.wardrobe.generation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.generation")
public record DmitriyGenerationProperties(
		String baseUrl
) {
}

