package com.stylish.wardrobe.generation;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GenerationClient {
	private final RestClient restClient;

	public GenerationClient(GenerationProperties props) {
		this.restClient = RestClient.builder()
				.baseUrl(props.baseUrl())
				.build();
	}

	public byte[] generateLook(String userPhotoObjectKey, List<String> itemObjectKeys) {
		GenerateRequest req = new GenerateRequest(userPhotoObjectKey, itemObjectKeys);
		return restClient.post()
				.uri("/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.IMAGE_PNG)
				.body(req)
				.retrieve()
				.body(byte[].class);
	}

	public record GenerateRequest(String userPhotoObjectKey, List<String> itemObjectKeys) {
	}
}

