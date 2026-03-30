package com.hrpilot.backend.common.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String provider = "local";
    private Local local = new Local();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        private String uploadDir = "uploads";
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region;
        private String endpoint;
        private String accessKey;
        private String secretKey;
    }
}
