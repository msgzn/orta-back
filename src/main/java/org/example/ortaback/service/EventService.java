package org.example.ortaback.service;

import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.geo.BoundingBox;
import org.example.ortaback.persistence.models.User;

import java.util.List;

/**
 * Use-case'ы карты — публичный контракт {@code EventService} для
 * геопространственной части ORTA.
 *
 * <p>CRUD-операции над событиями добавятся отдельной итерацией вместе с
 * {@code EventController}.
 */
public interface EventService {

    /**
     * События в радиусе от точки, отсортированные по возрастанию расстояния.
     * Применяет приватную маскировку координат для APPROXIMATE-событий
     * к viewer'у, не входящему в число участников.
     */
    List<EventMapMarkerDto> findNearbyEvents(double lat,
                                             double lng,
                                             double radiusMeters,
                                             User viewer);

    /**
     * События в видимой области карты с авто-выбором стратегии:
     * сырые маркеры при {@code total ≤ client-cluster-threshold},
     * иначе серверная кластеризация ({@code ST_ClusterKMeans}).
     */
    BoundingBoxResultDto findInBoundingBox(BoundingBox bbox, User viewer);

    /**
     * Явная серверная кластеризация — фронт хочет именно кластеры независимо
     * от количества точек.
     */
    List<ClusterDto> findClustersInBoundingBox(BoundingBox bbox, Integer zoom);

    /**
     * События в bbox как {@code GeoJSON FeatureCollection} — для прямой
     * подгрузки в Leaflet / Mapbox / Yandex / Google Maps.
     */
    GeoJsonFeatureCollectionDto findGeoJsonInBoundingBox(BoundingBox bbox, User viewer);
}
