package com.stylish.auth.controller;

import java.time.Instant;
import java.util.UUID;

import com.stylish.auth.dto.DmitriyLoginRequest;
import com.stylish.auth.dto.DmitriyLogoutRequest;
import com.stylish.auth.dto.DmitriyMeResponse;
import com.stylish.auth.dto.DmitriyRefreshRequest;
import com.stylish.auth.dto.DmitriyRegisterRequest;
import com.stylish.auth.dto.DmitriyTokenPairResponse;
import com.stylish.auth.dto.DmitriyUserResponse;
import com.stylish.auth.service.DmitriyAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Регистрация, логин, refresh / logout, текущий пользователь")
public class DmitriyAuthController {
	private final DmitriyAuthService authService;

	public DmitriyAuthController(DmitriyAuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@Operation(summary = "Регистрация нового пользователя")
	public DmitriyUserResponse register(@Valid @RequestBody DmitriyRegisterRequest req, HttpServletRequest http) {
		return authService.register(req.email(), req.password(), clientIp(http), userAgent(http));
	}

	@PostMapping("/login")
	@Operation(summary = "Логин по email/паролю, возвращает пару access+refresh токенов")
	public DmitriyTokenPairResponse login(@Valid @RequestBody DmitriyLoginRequest req, HttpServletRequest http) {
		return authService.login(req.email(), req.password(), Instant.now(), clientIp(http), userAgent(http));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Обмен refresh-токена на новую пару (ротация — старый отзывается)")
	public DmitriyTokenPairResponse refresh(@Valid @RequestBody DmitriyRefreshRequest req, HttpServletRequest http) {
		return authService.refresh(req.refreshToken(), Instant.now(), clientIp(http), userAgent(http));
	}

	@PostMapping("/logout")
	@Operation(summary = "Отзыв refresh-токена")
	public void logout(@Valid @RequestBody DmitriyLogoutRequest req, HttpServletRequest http) {
		authService.logout(req.refreshToken(), Instant.now(), clientIp(http), userAgent(http));
	}

	private static String clientIp(HttpServletRequest http) {
		String xff = http.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			int comma = xff.indexOf(',');
			return (comma > 0 ? xff.substring(0, comma) : xff).trim();
		}
		return http.getRemoteAddr();
	}

	private static String userAgent(HttpServletRequest http) {
		return http.getHeader("User-Agent");
	}

	@GetMapping("/me")
	@Operation(summary = "Данные текущего пользователя (по access-токену)")
	@SecurityRequirement(name = "bearer")
	public DmitriyMeResponse me(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return new DmitriyMeResponse(userId, jwt.getClaimAsString("email"));
	}
}

