package com.stylish.generation.controller;

import com.stylish.generation.dto.GenerateRequest;
import com.stylish.generation.service.StubGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name = "Generation", description = "Стаб-генерация PNG-картинки лука по входным URL")
public class GenerationController {
	private final StubGenerationService stubGenerationService;

	public GenerationController(StubGenerationService stubGenerationService) {
		this.stubGenerationService = stubGenerationService;
	}

	@PostMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
	@Operation(summary = "Сгенерировать PNG-плейсхолдер (заглушка вместо реальной ML-модели)")
	public ResponseEntity<byte[]> generate(@Valid @RequestBody GenerateRequest req) {
		byte[] png = stubGenerationService.generatePngPlaceholder(768);
		return ResponseEntity.ok()
				.contentType(MediaType.IMAGE_PNG)
				.body(png);
	}
}

