package org.example.ortaback.dto.geo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Envelope-ответ для {@code GET /api/v1/map/events/bbox}.
 *
 * <p>Сервер сам решает, отдавать сырые маркеры или кластеры — на основании
 * порога {@code application.geo.client-cluster-threshold}. Фронт смотрит в
 * {@link #type()}:
 * <ul>
 *   <li>{@code MARKERS} — заполнено {@link #markers()};</li>
 *   <li>{@code CLUSTERS} — заполнено {@link #clusters()}.</li>
 * </ul>
 *
 * <p>{@code total} — общее число активных событий в bbox (одинаковое для
 * обоих режимов), удобно показать в UI «найдено N событий».
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BoundingBoxResultDto(
        Kind type,
        long total,
        List<EventMapMarkerDto> markers,
        List<ClusterDto> clusters
) {

    public enum Kind { MARKERS, CLUSTERS }

    public static BoundingBoxResultDto markers(long total, List<EventMapMarkerDto> markers) {
        return new BoundingBoxResultDto(Kind.MARKERS, total, markers, null);
    }

    public static BoundingBoxResultDto clusters(long total, List<ClusterDto> clusters) {
        return new BoundingBoxResultDto(Kind.CLUSTERS, total, null, clusters);
    }
}
