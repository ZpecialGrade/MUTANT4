package com.stylish.wardrobe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
	@Bean
	OpenAPI wardrobeOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Stylish Wardrobe Service API")
						.description("Профили, вещи, фото пользователя, генерация луков, активити.")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes("bearer", new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
