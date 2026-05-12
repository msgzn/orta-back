package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.geo.BoundingBox;
import org.example.ortaback.geo.annotation.ValidLatitude;
import org.example.ortaback.geo.annotation.ValidLongitude;
import org.example.ortaback.geo.annotation.ValidRadiusMeters;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.EventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-эндпоинты карты ORTA.
 *
 * <p>Все маршруты — {@code GET}, без побочных эффектов. Авторизация
 * требуется (см. {@code SecurityConfig}); приватная маскировка
 * APPROXIMATE-координат идёт от лица аутентифицированного пользователя.
 */
@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
@Validated
@Tag(name = "Map", description = "Геопространственные запросы карты")
public class MapController {

    private final EventService eventService;
    private final GeoProperties geoProperties;

    /**
     * События в радиусе вокруг точки.
     *
     * <p>Если {@code radius} опущен — берётся {@code searchRadiusKm} пользователя
     * (или дефолт {@code application.geo.default-search-radius-km}).
     */
    @Operation(summary = "События рядом с точкой")
    @GetMapping("/events/nearby")
    public List<EventMapMarkerDto> nearby(
            @RequestParam @ValidLatitude  Double lat,
            @RequestParam @ValidLongitude Double lng,
            @RequestParam(required = false) @ValidRadiusMeters Double radius,
            @AuthenticationPrincipal User currentUser) {

        double effectiveRadius = radius != null
                ? radius
                : userOrDefaultRadiusMeters(currentUser);

        return eventService.findNearbyEvents(lat, lng, effectiveRadius, currentUser);
    }

    /**
     * События в видимой области карты — сервер сам решает «маркеры или
     * кластеры» по порогу {@code application.geo.client-cluster-threshold}.
     */
    @Operation(summary = "События в видимой области карты")
    @GetMapping("/events/bbox")
    public BoundingBoxResultDto bbox(
            @RequestParam @ValidLatitude  Double minLat,
            @RequestParam @ValidLongitude Double minLng,
            @RequestParam @ValidLatitude  Double maxLat,
            @RequestParam @ValidLongitude Double maxLng,
            @AuthenticationPrincipal User currentUser) {

        BoundingBox box = BoundingBox.of(minLat, minLng, maxLat, maxLng);
        return eventService.findInBoundingBox(box, currentUser);
    }

    /**
     * Принудительная серверная кластеризация (фронту нужны именно кластеры).
     */
    @Operation(summary = "Серверная кластеризация (ST_ClusterKMeans)")
    @GetMapping("/events/clusters")
    public List<ClusterDto> clusters(
            @RequestParam @ValidLatitude  Double minLat,
            @RequestParam @ValidLongitude Double minLng,
            @RequestParam @ValidLatitude  Double maxLat,
            @RequestParam @ValidLongitude Double maxLng,
            @RequestParam(required = false) Integer zoom) {

        BoundingBox box = BoundingBox.of(minLat, minLng, maxLat, maxLng);
        return eventService.findClustersInBoundingBox(box, zoom);
    }

    /**
     * События в bbox в формате GeoJSON {@code FeatureCollection} (RFC 7946)
     * — для прямой подгрузки в Leaflet / Mapbox / Yandex / Google Maps.
     */
    @Operation(summary = "События в bbox в формате GeoJSON")
    @GetMapping(value = "/events/geojson", produces = "application/geo+json")
    public GeoJsonFeatureCollectionDto geoJson(
            @RequestParam @ValidLatitude  Double minLat,
            @RequestParam @ValidLongitude Double minLng,
            @RequestParam @ValidLatitude  Double maxLat,
            @RequestParam @ValidLongitude Double maxLng,
            @AuthenticationPrincipal User currentUser) {

        BoundingBox box = BoundingBox.of(minLat, minLng, maxLat, maxLng);
        return eventService.findGeoJsonInBoundingBox(box, currentUser);
    }

    // -----------------------------------------------------------------------

    private double userOrDefaultRadiusMeters(User user) {
        Integer perUser = user != null ? user.getSearchRadiusKm() : null;
        int km = perUser != null ? perUser : geoProperties.defaultSearchRadiusKm();
        return km * 1000.0;
    }
}
