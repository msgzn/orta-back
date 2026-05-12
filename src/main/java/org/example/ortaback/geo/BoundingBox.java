package org.example.ortaback.geo;

import org.example.ortaback.exception.InvalidCoordinatesException;

/**
 * Immutable bounding box в координатах WGS 84.
 *
 * <p>Хранит явные углы (юг-запад / северо-восток). Используется как DTO между
 * слоями (контроллер → сервис → репозиторий) для запросов карты.
 *
 * <p>Сама пара (lat/lng) валидируется через {@link #of(double, double, double, double)}.
 */
public record BoundingBox(
        double minLat,
        double minLng,
        double maxLat,
        double maxLng
) {

    /**
     * Конструирующий метод с валидацией. Бросает {@link InvalidCoordinatesException}
     * при недопустимых значениях.
     */
    public static BoundingBox of(double minLat, double minLng, double maxLat, double maxLng) {
        if (minLat < -90 || minLat > 90 || maxLat < -90 || maxLat > 90) {
            throw new InvalidCoordinatesException("Bounding box latitudes must be in [-90, 90]");
        }
        if (minLng < -180 || minLng > 180 || maxLng < -180 || maxLng > 180) {
            throw new InvalidCoordinatesException("Bounding box longitudes must be in [-180, 180]");
        }
        if (minLat > maxLat) {
            throw new InvalidCoordinatesException(
                    "Bounding box minLat (%.4f) must be ≤ maxLat (%.4f)".formatted(minLat, maxLat));
        }
        if (minLng > maxLng) {
            throw new InvalidCoordinatesException(
                    "Bounding box minLng (%.4f) must be ≤ maxLng (%.4f)".formatted(minLng, maxLng));
        }
        return new BoundingBox(minLat, minLng, maxLat, maxLng);
    }
}
