package com.hrpilot.backend.common.storage;

import org.springframework.core.io.InputStreamResource;

public record StoredFileContent(
    InputStreamResource resource,
    String contentType,
    long contentLength,
    String filename
) {
}
