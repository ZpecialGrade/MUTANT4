package com.stylish.auth.controller;

import java.time.Instant;
import java.util.UUID;

import com.stylish.auth.dto.LoginRequest;
import com.stylish.auth.dto.LogoutRequest;
import com.stylish.auth.dto.MeResponse;
import com.stylish.auth.dto.RefreshRequest;
import com.stylish.auth.dto.RegisterRequest;
import com.stylish.auth.dto.TokenPairResponse;
import com.stylish.auth.dto.UserResponse;
import com.stylish.auth.service.AuthService;

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
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public UserResponse register(@Valid @RequestBody RegisterRequest req) {
		return authService.register(req.email(), req.password());
	}

	@PostMapping("/login")
	public TokenPairResponse login(@Valid @RequestBody LoginRequest req) {
		return authService.login(req.email(), req.password(), Instant.now());
	}

	@PostMapping("/refresh")
	public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req) {
		return authService.refresh(req.refreshToken(), Instant.now());
	}

	@PostMapping("/logout")
	public void logout(@Valid @RequestBody LogoutRequest req) {
		authService.logout(req.refreshToken(), Instant.now());
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return new MeResponse(userId, jwt.getClaimAsString("email"));
	}
}

