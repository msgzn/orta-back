package org.example.ortaback.geo;

import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonGeometryDto;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Конвертер JTS {@link Point} / списка фич → GeoJSON DTO (RFC 7946).
 *
 * <p>Сознательно не делаем здесь конвертацию доменных сущностей
 * ({@code Event}) — это уровень знания о домене, который не должен жить в
 * {@code geo} пакете. Маппер событий в фичи живёт в {@code mapper/}
 * (следующая итерация) и зовёт сюда только методы общего назначения.
 */
public final class GeoJsonConverter {

    private GeoJsonConverter() { /* utility class */ }

    /**
     * Преобразует JTS {@link Point} в GeoJSON Point.
     * Возвращает {@code null}, если на входе {@code null} — удобно при
     * маппинге опциональных гео-полей.
     */
    public static GeoJsonGeometryDto toGeometry(Point point) {
        if (point == null) {
            return null;
        }
        return GeoJsonGeometryDto.point(point.getY(), point.getX());
    }

    public static GeoJsonGeometryDto toGeometry(double lat, double lng) {
        return GeoJsonGeometryDto.point(lat, lng);
    }

    /**
     * Собирает GeoJSON Feature из id, геометрии и произвольной карты свойств.
     */
    public static GeoJsonFeatureDto toFeature(Object id,
                                              Point point,
                                              Map<String, Object> properties) {
        return GeoJsonFeatureDto.of(
                id,
                toGeometry(point),
                properties != null ? properties : Collections.emptyMap()
        );
    }

    public static GeoJsonFeatureCollectionDto toFeatureCollection(List<GeoJsonFeatureDto> features) {
        return GeoJsonFeatureCollectionDto.of(
                features != null ? features : new ArrayList<>()
        );
    }
}
