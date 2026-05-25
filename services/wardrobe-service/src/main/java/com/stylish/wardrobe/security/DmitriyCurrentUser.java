package com.stylish.wardrobe.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class DmitriyCurrentUser {
	public UUID userId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			throw new IllegalStateException("No JWT principal in security context");
		}
		return UUID.fromString(jwt.getSubject());
	}
}

