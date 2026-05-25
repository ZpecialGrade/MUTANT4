package com.stylish.wardrobe.storage;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class DmitriyS3Config {
	@Bean
	S3Client s3Client(DmitriyS3Properties props) {
		return S3Client.builder()
				.endpointOverride(URI.create(props.endpoint()))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(props.accessKey(), props.secretKey())
				))
				.region(Region.of(props.region()))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}
}

