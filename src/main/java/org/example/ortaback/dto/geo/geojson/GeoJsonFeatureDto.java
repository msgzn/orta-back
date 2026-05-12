package org.example.ortaback.dto.geo.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * GeoJSON Feature (RFC 7946 §3.2).
 *
 * <p>{@code properties} держим открытой картой — туда сервисный слой кладёт
 * id события, заголовок, число участников, имя иконки категории и т.п.
 * Это удобно для прямой подгрузки в Leaflet / Mapbox / Yandex / Google Maps,
 * которым важна именно структура GeoJSON, а не наш внутренний домен.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoJsonFeatureDto(
        String type,
        Object id,
        GeoJsonGeometryDto geometry,
        Map<String, Object> properties
) {

    public static GeoJsonFeatureDto of(Object id,
                                       GeoJsonGeometryDto geometry,
                                       Map<String, Object> properties) {
        return new GeoJsonFeatureDto("Feature", id, geometry, properties);
    }
}
