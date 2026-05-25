package com.stylish.auth.event;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/events")
@Tag(name = "Auth Events", description = "История auth-событий текущего пользователя (MongoDB)")
@SecurityRequirement(name = "bearer")
public class DmitriyAuthEventController {

	private final DmitriyAuthEventService authEventService;

	public DmitriyAuthEventController(DmitriyAuthEventService authEventService) {
		this.authEventService = authEventService;
	}

	@GetMapping("/me")
	@Operation(summary = "Последние N auth-событий текущего пользователя")
	public List<DmitriyAuthEventResponse> me(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "50") int limit
	) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return authEventService.listForUser(userId, limit).stream()
				.map(DmitriyAuthEventResponse::of)
				.toList();
	}
}
