package com.hicct3.projectfinder.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloudflare.r2")
public class R2Properties {
    private boolean enabled = false;
    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
    private String publicBaseUrl;
    private String objectPrefix = "posts";
    private long maxImageSizeBytes = 5 * 1024 * 1024;
    private int maxImagesPerRequest = 10;
    private List<String> allowedImageContentTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }
}
