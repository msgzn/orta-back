package org.example.ortaback.dto.geo.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * GeoJSON FeatureCollection (RFC 7946 §3.3) — корневой объект, который сервер
 * отдаёт фронту по {@code GET /api/v1/map/events/geojson}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoJsonFeatureCollectionDto(
        String type,
        List<GeoJsonFeatureDto> features
) {

    public static GeoJsonFeatureCollectionDto of(List<GeoJsonFeatureDto> features) {
        return new GeoJsonFeatureCollectionDto("FeatureCollection", features);
    }
}
