package com.hrpilot.backend.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileMetadata store(MultipartFile file, String subdirectory, String preferredFilename);

    default String store(MultipartFile file, String subdirectory) {
        return store(file, subdirectory, file.getOriginalFilename()).storageKey();
    }

    StoredFileMetadata store(byte[] content, String subdirectory, String filename, String contentType);

    StoredFileContent load(String storageKey, String filename);

    void delete(String filePath);
}
