package org.example.ortaback.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Бин {@link S3Client} (AWS SDK v2), сконфигурированный по
 * {@link StorageProperties}.
 *
 * <p>Стратегия — fail-open: если {@code accessKey}/{@code secretKey}
 * пустые, бин всё равно создаётся, но с {@link AnonymousCredentialsProvider}.
 * Приложение поднимется; любая попытка реальной загрузки/удаления упадёт
 * на стороне S3 уже в рантайме (403) с понятным
 * {@code FileUploadException}, а не валит весь Spring-контекст на старте.
 *
 * <p>Это удобно для dev-окружения, где S3 не нужен: фронт может тестировать
 * остальной API, не настраивая Yandex/MinIO. В проде ops выставит env-vars.
 *
 * <p>Если {@code endpoint} пустой — SDK сам выберет AWS-нативную точку
 * по региону. Заполнение endpoint обязательно для всех не-AWS провайдеров
 * (MinIO, Yandex, DigitalOcean и т.п.).
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        StorageProperties.S3 s3 = storageProperties.s3();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.region()))
                .credentialsProvider(credentialsProviderFor(s3))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.pathStyleAccess())
                        .build());

        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }

        return builder.build();
    }

    private static AwsCredentialsProvider credentialsProviderFor(StorageProperties.S3 s3) {
        if (isBlank(s3.accessKey()) || isBlank(s3.secretKey())) {
            log.warn("S3 credentials are not configured (S3_ACCESS_KEY / S3_SECRET_KEY empty); "
                    + "uploads/deletes will fail at runtime until env vars are set");
            return AnonymousCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3.accessKey(), s3.secretKey()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
