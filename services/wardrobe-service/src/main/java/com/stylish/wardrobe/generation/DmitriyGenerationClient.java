package com.stylish.wardrobe.generation;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DmitriyGenerationClient {
	private final RestClient restClient;

	public DmitriyGenerationClient(DmitriyGenerationProperties props) {
		this.restClient = RestClient.builder()
				.baseUrl(props.baseUrl())
				.build();
	}

	public byte[] generateLook(String userPhotoObjectKey, List<String> itemObjectKeys) {
		DmitriyGenerateRequest req = new DmitriyGenerateRequest(userPhotoObjectKey, itemObjectKeys);
		return restClient.post()
				.uri("/generate")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.IMAGE_PNG)
				.body(req)
				.retrieve()
				.body(byte[].class);
	}

	public record DmitriyGenerateRequest(String userPhotoObjectKey, List<String> itemObjectKeys) {
	}
}

