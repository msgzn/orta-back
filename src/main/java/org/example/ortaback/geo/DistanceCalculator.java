package org.example.ortaback.geo;

import org.locationtech.jts.geom.Point;

/**
 * Вычисление расстояния между двумя точками без обращения к БД — по формуле
 * гаверсинуса.
 *
 * <p>Используется, когда событие уже загружено в память и нужно показать
 * пользователю «X км от вас», или для постфильтрации/сортировки на сервисном
 * слое после KNN-выборки через {@code <->} (planar-расстояние которого
 * корректно для KNN-порядка, но не для отображения в км).
 *
 * <p>Точность: средняя ошибка ≈ 0.5% за счёт сферической модели Земли
 * (а не эллипсоида WGS-84). Для UI «0.5 км / 2.3 км» этого с запасом.
 */
public final class DistanceCalculator {

    /** Средний радиус Земли, метры (IUGG mean radius). */
    public static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private DistanceCalculator() { /* utility class */ }

    /**
     * Расстояние между двумя точками в метрах по формуле гаверсинуса.
     */
    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sinDLat2 = Math.sin(dLat / 2.0);
        double sinDLng2 = Math.sin(dLng / 2.0);

        double a = sinDLat2 * sinDLat2
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad) * sinDLng2 * sinDLng2;

        return 2.0 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public static double haversineMeters(Point a, Point b) {
        return haversineMeters(a.getY(), a.getX(), b.getY(), b.getX());
    }
}
