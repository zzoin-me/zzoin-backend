package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.config.R2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

    public void validatePostImageUrls(Long userId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        validateConfiguration();
        String expectedPrefix = normalizedPublicBaseUrl() + "/" + normalizedObjectPrefix() + "/" + userId + "/";
        if (imageUrls.stream().anyMatch(url -> url == null || !url.startsWith(expectedPrefix))) {
            throw new GeneralException(ErrorCode.INVALID_IMAGE_URL);
        }
    }

    public void deleteManagedImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        try {
            validateConfiguration();
            try (S3Client s3Client = createClient()) {
                imageUrls.stream()
                        .filter(this::isManagedImageUrl)
                        .map(this::extractObjectKey)
                        .forEach(objectKey -> s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(properties.getBucketName())
                                .key(objectKey)
                                .build()));
            }
        } catch (RuntimeException e) {
            log.warn("R2 image cleanup failed", e);
        }
    }

    private String uploadOne(S3Client s3Client, Long userId, MultipartFile image) {
        return uploadOne(s3Client, userId, image, null);
    }

    private String uploadOne(S3Client s3Client, Long userId, MultipartFile image, String directory) {
        validateImage(image);
        String objectKey = buildObjectKey(userId, image.getContentType(), directory);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(objectKey)
                    .contentType(image.getContentType())
                    .contentLength(image.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
            return normalizedPublicBaseUrl() + "/" + objectKey;
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
        if (!hasValidImageSignature(image, contentType.toLowerCase(Locale.ROOT))) {
            throw new GeneralException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private boolean hasValidImageSignature(MultipartFile image, String contentType) {
        try {
            byte[] header = image.getInputStream().readNBytes(12);
            return switch (contentType) {
                case "image/jpeg" -> matches(header, 0xFF, 0xD8, 0xFF);
                case "image/png" -> matches(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "image/gif" -> matches(header, 0x47, 0x49, 0x46, 0x38)
                        && header.length >= 6
                        && (header[4] == 0x37 || header[4] == 0x39)
                        && header[5] == 0x61;
                case "image/webp" -> matches(header, 0x52, 0x49, 0x46, 0x46)
                        && header.length >= 12
                        && header[8] == 0x57
                        && header[9] == 0x45
                        && header[10] == 0x42
                        && header[11] == 0x50;
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    private boolean matches(byte[] bytes, int... expected) {
        if (bytes.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private String buildObjectKey(Long userId, String contentType, String directory) {
        String extension = extensionForContentType(contentType);
        String prefix = normalizedObjectPrefix();
        String userPath = directory == null || directory.isBlank()
                ? String.valueOf(userId)
                : directory.replaceAll("^/+|/+$", "") + "/" + userId;
        return prefix + "/" + userPath + "/" + UUID.randomUUID() + extension;
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
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

    private boolean isManagedImageUrl(String imageUrl) {
        return imageUrl != null && imageUrl.startsWith(normalizedPublicBaseUrl() + "/");
    }

    private String extractObjectKey(String imageUrl) {
        return imageUrl.substring(normalizedPublicBaseUrl().length() + 1);
    }

    private String normalizedPublicBaseUrl() {
        return properties.getPublicBaseUrl().replaceAll("/+$", "");
    }

    private String normalizedObjectPrefix() {
        return properties.getObjectPrefix().replaceAll("^/+|/+$", "");
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
