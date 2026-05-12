package org.example.ortaback.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
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
 * <p>Если {@code endpoint} пустой — SDK сам выберет AWS-нативную точку
 * по региону. Заполнение endpoint обязательно для всех не-AWS провайдеров
 * (MinIO, Yandex, DigitalOcean и т.п.).
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties storageProperties;

    @Bean
    public S3Client s3Client() {
        StorageProperties.S3 s3 = storageProperties.s3();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3.pathStyleAccess())
                        .build());

        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }

        return builder.build();
    }
}
