package com.stylish.wardrobe.storage;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3StorageService implements StorageService {
	private final S3Client s3;
	private final S3Properties props;

	public S3StorageService(S3Client s3, S3Properties props) {
		this.s3 = s3;
		this.props = props;
		ensureBucket();
	}

	@Override
	public String putObject(String objectKey, String contentType, InputStream input, long contentLength) {
		try {
			PutObjectRequest req = PutObjectRequest.builder()
					.bucket(props.bucket())
					.key(objectKey)
					.contentType(contentType)
					.build();
			s3.putObject(req, RequestBody.fromInputStream(input, contentLength));
			return objectKey;
		} catch (S3Exception e) {
			throw new StorageException("Failed to upload object to storage", e);
		}
	}

	@Override
	public StoredObject getObject(String objectKey) {
		try {
			GetObjectRequest req = GetObjectRequest.builder()
					.bucket(props.bucket())
					.key(objectKey)
					.build();
			ResponseInputStream<GetObjectResponse> stream = s3.getObject(req);
			GetObjectResponse resp = stream.response();
			String contentType = resp.contentType() != null ? resp.contentType() : "application/octet-stream";
			long len = resp.contentLength() != null ? resp.contentLength() : -1L;
			return new StoredObject(objectKey, contentType, len, stream);
		} catch (S3Exception e) {
			throw new StorageException("Failed to download object from storage", e);
		}
	}

	@Override
	public void deleteObject(String objectKey) {
		try {
			DeleteObjectRequest req = DeleteObjectRequest.builder()
					.bucket(props.bucket())
					.key(objectKey)
					.build();
			s3.deleteObject(req);
		} catch (S3Exception e) {
			throw new StorageException("Failed to delete object from storage", e);
		}
	}

	private void ensureBucket() {
		try {
			s3.headBucket(HeadBucketRequest.builder().bucket(props.bucket()).build());
		} catch (NoSuchBucketException e) {
			try {
				s3.createBucket(b -> b.bucket(props.bucket()));
			} catch (S3Exception ex) {
				throw new StorageException("Failed to create bucket " + props.bucket(), ex);
			}
		} catch (S3Exception e) {
			throw new StorageException("Failed to access bucket " + props.bucket(), e);
		}
	}
}

