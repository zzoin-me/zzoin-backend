package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class R2ImageStorageService {

    private static final Region R2_REGION = Region.of("auto");

    private final R2Properties properties;

    public List<String> uploadPostImages(Long userId, List<MultipartFile> images) {
        validateConfiguration();
        if (images == null || images.isEmpty()) {
            throw new GeneralException(ErrorCode.FILE_EMPTY);
        }
        if (images.size() > properties.getMaxImagesPerRequest()) {
            throw new GeneralException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        images.forEach(this::validateImage);

        try (S3Client s3Client = createClient()) {
            return images.stream()
                    .map(image -> uploadOne(s3Client, userId, image))
                    .toList();
        }
    }

    public String uploadProfileImage(Long userId, MultipartFile image) {
        validateConfiguration();
        validateImage(image);

        try (S3Client s3Client = createClient()) {
            return uploadOne(s3Client, userId, image, "profiles");
        }
    }

    private String uploadOne(S3Client s3Client, Long userId, MultipartFile image) {
        return uploadOne(s3Client, userId, image, null);
    }

    private String uploadOne(S3Client s3Client, Long userId, MultipartFile image, String directory) {
        validateImage(image);
        String objectKey = buildObjectKey(userId, image.getOriginalFilename(), directory);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(image.getContentType())
                    .contentLength(image.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
            return properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
        } catch (IOException e) {
            throw new GeneralException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(ErrorCode.FILE_EMPTY);
        }
        if (image.getSize() > properties.getMaxImageSizeBytes()) {
            throw new GeneralException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = image.getContentType();
        if (contentType == null || !properties.getAllowedImageContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new GeneralException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String buildObjectKey(Long userId, String originalFilename) {
        return buildObjectKey(userId, originalFilename, null);
    }

    private String buildObjectKey(Long userId, String originalFilename, String directory) {
        String extension = extractExtension(originalFilename);
        String prefix = properties.getObjectPrefix().replaceAll("^/+|/+$", "");
        String userPath = directory == null || directory.isBlank()
                ? String.valueOf(userId)
                : directory.replaceAll("^/+|/+$", "") + "/" + userId;
        return prefix + "/" + userPath + "/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private S3Client createClient() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKeyId(),
                properties.getSecretAccessKey()
        );
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(R2_REGION)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()
                || isBlank(properties.getAccountId())
                || isBlank(properties.getAccessKeyId())
                || isBlank(properties.getSecretAccessKey())
                || isBlank(properties.getBucketName())
                || isBlank(properties.getPublicBaseUrl())) {
            throw new GeneralException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
