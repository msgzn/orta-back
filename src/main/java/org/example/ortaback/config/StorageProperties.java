package org.example.ortaback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Биндинг секции {@code application.storage.*}.
 *
 * <p>Конкретный provider определяется только {@code endpoint}/{@code region}
 * и наличием {@code pathStyleAccess}. Один и тот же код работает с AWS S3,
 * MinIO, Yandex Object Storage, DigitalOcean Spaces, Cloudflare R2 и т.д.
 */
@ConfigurationProperties(prefix = "application.storage")
public record StorageProperties(
        S3 s3
) {

    public record S3(
            String endpoint,
            String region,
            String bucket,
            String accessKey,
            String secretKey,

            /**
             * URL-префикс для собранных «открытых» URL загруженных файлов.
             * Должен совпадать с тем, что видит браузер, не с приватным
             * endpoint'ом. Пример для Yandex: https://<bucket>.storage.yandexcloud.net
             */
            String publicBaseUrl,

            /**
             * path-style: {@code https://endpoint/bucket/key}; virtual-host:
             * {@code https://bucket.endpoint/key}. Для MinIO и Yandex обычно
             * нужен path-style; для AWS native — virtual-host.
             */
            boolean pathStyleAccess,

            /** Жёсткий лимит размера одного файла (байты). */
            long maxUploadBytes,

            /**
             * Whitelist MIME-типов. Запросы с не-whitelisted Content-Type
             * отклоняются — это первичная защита от загрузки исполняемых
             * файлов и SVG с JS.
             */
            List<String> allowedContentTypes
    ) {}
}
