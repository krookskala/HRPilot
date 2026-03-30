package com.hrpilot.backend.common.storage;

public record StoredFileMetadata(
    String storageKey,
    String originalFilename,
    String contentType,
    long contentLength
) {
}
