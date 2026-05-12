package org.example.ortaback.storage;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.config.StorageProperties;
import org.example.ortaback.exception.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация {@link StorageService} поверх AWS SDK v2.
 *
 * <p>Стратегия публичности: каждый загруженный объект помечается
 * {@code ACL = public-read}, чтобы возвращаемый URL работал без подписи.
 * Это требует, чтобы в bucket-настройках провайдера были разрешены ACL и
 * публичный read (для AWS native — снять «Block all public access»; для
 * Yandex/MinIO работает из коробки).
 */
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    /** MIME-type → расширение имени файла. */
    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png",  "png",
            "image/webp", "webp"
    );

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    // -----------------------------------------------------------------------

    @Override
    public UploadedFile upload(MultipartFile file, String folder) {
        validate(file);

        String key = buildKey(folder, file.getContentType());
        StorageProperties.S3 s3 = storageProperties.s3();

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3.bucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .acl(ObjectCannedACL.PUBLIC_READ)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException ex) {
            throw new FileUploadException("Failed to read upload stream", ex);
        } catch (S3Exception ex) {
            log.error("S3 putObject failed for key={}: {}", key, ex.awsErrorDetails().errorMessage(), ex);
            throw new FileUploadException("Failed to store file", ex);
        }

        return new UploadedFile(key, buildPublicUrl(key));
    }

    @Override
    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        String prefix = storageProperties.s3().publicBaseUrl();
        if (!publicUrl.startsWith(prefix)) {
            // объект не наш — игнорируем, не лезем чужие URL'ы трогать
            return;
        }
        String key = publicUrl.substring(prefix.length());
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storageProperties.s3().bucket())
                    .key(key)
                    .build());
        } catch (S3Exception ex) {
            // Удаление — best-effort: не валим пользовательскую операцию,
            // если S3 моргнул. Лог + продолжаем.
            log.warn("S3 deleteObject failed for key={}: {}", key, ex.awsErrorDetails().errorMessage());
        }
    }

    // -----------------------------------------------------------------------

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("Uploaded file is empty");
        }
        StorageProperties.S3 s3 = storageProperties.s3();

        if (file.getSize() > s3.maxUploadBytes()) {
            throw new FileUploadException(
                    "File exceeds the %d byte limit".formatted(s3.maxUploadBytes()));
        }
        String contentType = file.getContentType();
        if (contentType == null
                || s3.allowedContentTypes() == null
                || !s3.allowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new FileUploadException(
                    "Unsupported content type: " + contentType);
        }
    }

    private static String buildKey(String folder, String contentType) {
        String ext = EXTENSION_BY_TYPE.getOrDefault(contentType, "bin");
        String safeFolder = StringUtils.hasText(folder) ? folder.replaceAll("[^a-zA-Z0-9/_-]", "") : "misc";
        return "%s/%s.%s".formatted(safeFolder, UUID.randomUUID(), ext);
    }

    private String buildPublicUrl(String key) {
        String base = storageProperties.s3().publicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + key;
    }
}
