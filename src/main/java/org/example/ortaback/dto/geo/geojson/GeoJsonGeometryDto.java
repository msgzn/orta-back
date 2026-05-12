package org.example.ortaback.dto.geo.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * GeoJSON Geometry (RFC 7946 §3.1).
 *
 * <p>Поддерживаем только {@code Point} — других геометрий у нас в API нет.
 * Координаты — массив {@code [longitude, latitude]} (порядок диктуется RFC,
 * а не интуицией; см. §4 RFC 7946).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoJsonGeometryDto(
        String type,
        double[] coordinates
) {

    public static GeoJsonGeometryDto point(double lat, double lng) {
        return new GeoJsonGeometryDto("Point", new double[]{lng, lat});
    }
}
