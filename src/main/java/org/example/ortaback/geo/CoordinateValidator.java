package org.example.ortaback.geo;

import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.exception.InvalidCoordinatesException;
import org.springframework.stereotype.Component;

/**
 * Программный валидатор широты, долготы и радиуса поиска.
 *
 * <p>Используется в местах, где Bean Validation неуместен (внутри сервисов,
 * при обработке параметров, пришедших не через DTO). Контроллеры по
 * возможности используют аннотации {@link org.example.ortaback.geo.annotation.ValidLatitude},
 * {@link org.example.ortaback.geo.annotation.ValidLongitude},
 * {@link org.example.ortaback.geo.annotation.ValidRadiusMeters}.
 */
@Component
public class CoordinateValidator {

    public static final double LAT_MIN = -90.0;
    public static final double LAT_MAX =  90.0;
    public static final double LNG_MIN = -180.0;
    public static final double LNG_MAX =  180.0;

    private final GeoProperties geoProperties;

    public CoordinateValidator(GeoProperties geoProperties) {
        this.geoProperties = geoProperties;
    }

    /**
     * @throws InvalidCoordinatesException если широта вне {@code [-90, 90]}
     *         или долгота вне {@code [-180, 180]}
     */
    public void validateLatLng(double lat, double lng) {
        if (lat < LAT_MIN || lat > LAT_MAX) {
            throw new InvalidCoordinatesException(
                    "Latitude must be in [%.1f, %.1f] but was %s".formatted(LAT_MIN, LAT_MAX, lat));
        }
        if (lng < LNG_MIN || lng > LNG_MAX) {
            throw new InvalidCoordinatesException(
                    "Longitude must be in [%.1f, %.1f] but was %s".formatted(LNG_MIN, LNG_MAX, lng));
        }
    }

    /**
     * Валидация радиуса поиска (метры). Радиус должен быть строго положительным
     * и не превышать {@code application.geo.max-search-radius-km * 1000}.
     */
    public void validateRadiusMeters(double radiusMeters) {
        if (radiusMeters <= 0) {
            throw new InvalidCoordinatesException(
                    "Search radius must be > 0 meters but was " + radiusMeters);
        }
        double maxMeters = geoProperties.maxSearchRadiusKm() * 1000.0;
        if (radiusMeters > maxMeters) {
            throw new InvalidCoordinatesException(
                    "Search radius %.0f m exceeds the maximum of %.0f m (%d km)"
                            .formatted(radiusMeters, maxMeters, geoProperties.maxSearchRadiusKm()));
        }
    }

    /**
     * Валидация bbox: минимальные значения не больше максимальных и каждая
     * сторона лежит в допустимом диапазоне.
     */
    public void validateBoundingBox(double minLat, double minLng, double maxLat, double maxLng) {
        validateLatLng(minLat, minLng);
        validateLatLng(maxLat, maxLng);
        if (minLat > maxLat) {
            throw new InvalidCoordinatesException(
                    "Bounding box minLat (%.4f) must be ≤ maxLat (%.4f)".formatted(minLat, maxLat));
        }
        if (minLng > maxLng) {
            throw new InvalidCoordinatesException(
                    "Bounding box minLng (%.4f) must be ≤ maxLng (%.4f)".formatted(minLng, maxLng));
        }
    }
}
