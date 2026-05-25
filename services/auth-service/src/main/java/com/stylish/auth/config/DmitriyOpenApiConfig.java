package com.stylish.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DmitriyOpenApiConfig {
	@Bean
	OpenAPI authOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Stylish Auth Service API")
						.description("Регистрация, логин, refresh токены, аудит auth-событий.")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes("bearer", new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
