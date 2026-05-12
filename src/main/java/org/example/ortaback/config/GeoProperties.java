package org.example.ortaback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding для секции {@code application.geo.*} из
 * {@code application.yml}.
 *
 * <p>Используется валидаторами координат / радиуса, сервисом карты
 * (порог клиентской кластеризации, лимиты «N ближайших») и
 * {@code PrivacyMasker} (шаг сетки для APPROXIMATE-событий).
 */
@ConfigurationProperties(prefix = "application.geo")
public record GeoProperties(
        /** SRID геометрий (всегда 4326 — WGS 84 / GPS). */
        int srid,

        /** Радиус поиска по умолчанию, км. */
        int defaultSearchRadiusKm,

        /** Жёсткий потолок радиуса, км — защита от чудовищных запросов. */
        int maxSearchRadiusKm,

        /**
         * Порог количества точек в видимой области карты: больше — переходим
         * на серверную кластеризацию.
         */
        int clientClusterThreshold,

        int nearestEventsDefaultLimit,
        int nearestEventsMaxLimit,
        Clustering clustering,
        Privacy privacy
) {

    public record Clustering(
            boolean enabled,
            Algorithm algorithm,
            int dbscanEpsMeters,
            int dbscanMinPoints,
            int kmeansMinClusters,
            int kmeansMaxClusters
    ) {
        public enum Algorithm { KMEANS, DBSCAN }
    }

    public record Privacy(
            /** Шаг сетки {@code ST_SnapToGrid} в метрах для APPROXIMATE-локаций. */
            int approximateGridMeters
    ) {}
}
