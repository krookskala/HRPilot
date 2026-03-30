package com.hrpilot.backend.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties storageProperties;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFileMetadata store(MultipartFile file, String subdirectory, String preferredFilename) {
        try {
            Path dir = Paths.get(storageProperties.getLocal().getUploadDir(), subdirectory);
            Files.createDirectories(dir);

            String originalFilename = preferredFilename != null && !preferredFilename.isBlank()
                ? preferredFilename
                : file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + extension;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {}", target);
            return new StoredFileMetadata(
                subdirectory + "/" + filename,
                originalFilename != null ? originalFilename : filename,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getSize()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public StoredFileMetadata store(byte[] content, String subdirectory, String filename, String contentType) {
        try {
            Path dir = Paths.get(storageProperties.getLocal().getUploadDir(), subdirectory);
            Files.createDirectories(dir);

            String extension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }

            String targetFilename = UUID.randomUUID() + extension;
            Path target = dir.resolve(targetFilename);
            Files.write(target, content);

            return new StoredFileMetadata(
                subdirectory + "/" + targetFilename,
                filename,
                contentType,
                content.length
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to store generated file", e);
        }
    }

    @Override
    public StoredFileContent load(String storageKey, String filename) {
        try {
            Path path = Paths.get(storageProperties.getLocal().getUploadDir(), storageKey);
            InputStream inputStream = Files.newInputStream(path);
            String contentType = Files.probeContentType(path);
            return new StoredFileContent(
                new InputStreamResource(inputStream),
                contentType != null ? contentType : "application/octet-stream",
                Files.size(path),
                filename
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path path = Paths.get(storageProperties.getLocal().getUploadDir(), filePath);
            Files.deleteIfExists(path);
            log.info("File deleted: {}", path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filePath, e);
        }
    }
}
