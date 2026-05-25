package com.stylish.generation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DmitriyOpenApiConfig {
	@Bean
	OpenAPI generationOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Stylish Generation Service API")
						.description("Стаб-сервис генерации финального лука по фото пользователя и вещам.")
						.version("v1"));
	}
}
