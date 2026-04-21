package com.stylish.wardrobe.look;

import java.util.UUID;

import com.stylish.wardrobe.look.dto.GenerateLookRequest;
import com.stylish.wardrobe.look.dto.LookResponse;
import com.stylish.wardrobe.look.dto.UpdateLookRequest;
import com.stylish.wardrobe.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/looks")
@Tag(name = "Looks", description = "Генерация итогового лука из фото пользователя и вещей")
@SecurityRequirement(name = "bearer")
public class LookController {
	private final CurrentUser currentUser;
	private final LookService lookService;

	public LookController(CurrentUser currentUser, LookService lookService) {
		this.currentUser = currentUser;
		this.lookService = lookService;
	}

	@PostMapping("/generate")
	@Operation(summary = "Сгенерировать лук по фото пользователя и набору вещей")
	public LookResponse generate(@Valid @RequestBody GenerateLookRequest req) {
		return lookService.generate(currentUser.userId(), req);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Получить сгенерированный лук по id")
	public LookResponse get(@PathVariable UUID id) {
		return lookService.get(currentUser.userId(), id);
	}

	@PatchMapping("/{id}")
	@Operation(summary = "Переименовать лук")
	public LookResponse rename(@PathVariable UUID id, @Valid @RequestBody UpdateLookRequest req) {
		return lookService.rename(currentUser.userId(), id, req);
	}
}

