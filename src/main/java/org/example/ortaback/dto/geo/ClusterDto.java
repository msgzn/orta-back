package org.example.ortaback.dto.geo;

/**
 * Один кластер событий, возвращаемый при серверной агрегации
 * ({@code ST_ClusterKMeans}). Координаты — центроид группы; {@code eventCount}
 * — сколько событий в неё вошло.
 */
public record ClusterDto(
        Integer clusterId,
        long eventCount,
        Double lat,
        Double lng
) {}
