package com.stylish.wardrobe.activity;

import java.util.List;

import com.stylish.wardrobe.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
@Tag(name = "Activities", description = "История действий пользователя в гардеробе (MongoDB)")
@SecurityRequirement(name = "bearer")
public class ActivityController {

	private final CurrentUser currentUser;
	private final ActivityService activityService;

	public ActivityController(CurrentUser currentUser, ActivityService activityService) {
		this.currentUser = currentUser;
		this.activityService = activityService;
	}

	@GetMapping("/me")
	@Operation(summary = "Последние N действий текущего пользователя")
	public List<ActivityResponse> me(@RequestParam(defaultValue = "50") int limit) {
		return activityService.listForUser(currentUser.userId(), limit).stream()
				.map(ActivityResponse::of)
				.toList();
	}
}
