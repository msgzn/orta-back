package org.example.ortaback.geo;

import org.example.ortaback.config.GeoProperties;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

/**
 * Маскировка точных координат для событий с
 * {@link org.example.ortaback.persistence.models.enums.LocationPrecision#APPROXIMATE}.
 *
 * <p>Java-эквивалент PostGIS-функции {@code ST_SnapToGrid}: точка
 * округляется до ближайшего узла сетки с шагом {@code approximateGridMeters}.
 * Применяется на сервисном слое, когда событие уже загружено в память
 * и нужно отдать «зону» вместо точной точки тому, кто не присоединился
 * к событию.
 *
 * <p>NB: сетка определяется в метрах, а координаты — в градусах. Длина одного
 * градуса широты ≈ 111 320 м постоянна; долготы — зависит от широты:
 * {@code 111 320 · cos(lat) м}. Поэтому шаг сетки по lng пересчитывается
 * на лету.
 */
@Component
public class PrivacyMasker {

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private final GeoProperties geoProperties;

    public PrivacyMasker(GeoProperties geoProperties) {
        this.geoProperties = geoProperties;
    }

    /**
     * Снэпит координаты к сетке с шагом из конфига. Для {@code null} возвращает
     * {@code null} (валидно для опциональных полей).
     */
    public Point snap(Point exact) {
        if (exact == null) {
            return null;
        }
        return snap(exact, geoProperties.privacy().approximateGridMeters());
    }

    public Point snap(Point exact, int gridMeters) {
        if (exact == null) {
            return null;
        }
        double lat = exact.getY();
        double lng = exact.getX();

        double gridLatDeg = gridMeters / METERS_PER_DEGREE_LAT;
        // Возле полюсов cos(lat) → 0, в выражении ниже это даст gridLngDeg → ∞ —
        // на практике события на 89°N маловероятны, но защитимся минимумом:
        double cosLat = Math.max(Math.cos(Math.toRadians(lat)), 1e-6);
        double gridLngDeg = gridMeters / (METERS_PER_DEGREE_LAT * cosLat);

        double snappedLat = Math.round(lat / gridLatDeg) * gridLatDeg;
        double snappedLng = Math.round(lng / gridLngDeg) * gridLngDeg;

        return GeoUtils.point(snappedLat, snappedLng);
    }
}
