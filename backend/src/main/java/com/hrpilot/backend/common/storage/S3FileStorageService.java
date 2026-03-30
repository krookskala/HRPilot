package com.hrpilot.backend.common.storage;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;
    private final S3Client s3Client;

    public S3FileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        StorageProperties.S3 s3 = storageProperties.getS3();

        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(s3.getRegion() != null && !s3.getRegion().isBlank() ? s3.getRegion() : "eu-central-1"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())
            ));

        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(s3.getEndpoint()));
        }

        this.s3Client = builder.build();
    }

    @Override
    public StoredFileMetadata store(MultipartFile file, String subdirectory, String preferredFilename) {
        try {
            String originalFilename = preferredFilename != null && !preferredFilename.isBlank()
                ? preferredFilename
                : file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String storageKey = subdirectory + "/" + UUID.randomUUID() + extension;
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucket())
                .key(storageKey)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return new StoredFileMetadata(
                storageKey,
                originalFilename != null ? originalFilename : storageKey,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file in S3", e);
        }
    }

    @Override
    public StoredFileMetadata store(byte[] content, String subdirectory, String filename, String contentType) {
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }

        String storageKey = subdirectory + "/" + UUID.randomUUID() + extension;
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(storageProperties.getS3().getBucket())
            .key(storageKey)
            .contentType(contentType)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        return new StoredFileMetadata(
            storageKey,
            filename,
            contentType,
            content.length
        );
    }

    @Override
    public StoredFileContent load(String storageKey, String filename) {
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
            .bucket(storageProperties.getS3().getBucket())
            .key(storageKey)
            .build();

        var metadata = s3Client.headObject(headRequest);
        var stream = s3Client.getObject(GetObjectRequest.builder()
            .bucket(storageProperties.getS3().getBucket())
            .key(storageKey)
            .build());

        return new StoredFileContent(
            new InputStreamResource(stream),
            metadata.contentType() != null ? metadata.contentType() : "application/octet-stream",
            metadata.contentLength(),
            filename
        );
    }

    @Override
    public void delete(String filePath) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(storageProperties.getS3().getBucket())
            .key(filePath)
            .build());
        log.info("Deleted S3 object {}", filePath);
    }

    @PreDestroy
    void destroy() {
        s3Client.close();
    }
}
