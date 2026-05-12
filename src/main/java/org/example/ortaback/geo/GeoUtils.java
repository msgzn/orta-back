package org.example.ortaback.geo;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Утилиты для работы с JTS-геометриями в координатах WGS 84 (SRID 4326).
 *
 * <p>{@link GeometryFactory} — потокобезопасный объект; держим один singleton
 * на всё приложение, чтобы не пересоздавать каждый раз.
 *
 * <p>NB по порядку координат: JTS использует {@code (X, Y)} = {@code (lng, lat)}.
 * Все методы этого класса принимают пары в человекочитаемом порядке
 * «широта, долгота» и сами разворачивают их для JTS.
 */
public final class GeoUtils {

    /** SRID для WGS 84 — глобальный стандарт GPS. */
    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoUtils() { /* utility class */ }

    /** Возвращает разделяемую фабрику геометрий, привязанную к SRID 4326. */
    public static GeometryFactory factory() {
        return GEOMETRY_FACTORY;
    }

    /**
     * Создаёт {@link Point} с указанной парой широта/долгота.
     *
     * @param lat широта, диапазон {@code [-90, 90]}
     * @param lng долгота, диапазон {@code [-180, 180]}
     * @return {@code Point} с {@code SRID = 4326}, готовый к записи в БД
     *         как {@code geometry(Point,4326)}
     */
    public static Point point(double lat, double lng) {
        Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        // createPoint наследует SRID фабрики, но на всякий случай явно:
        p.setSRID(SRID_WGS84);
        return p;
    }

    /** Извлекает широту из {@link Point}. */
    public static double latitude(Point point) {
        return point.getY();
    }

    /** Извлекает долготу из {@link Point}. */
    public static double longitude(Point point) {
        return point.getX();
    }
}
