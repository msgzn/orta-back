package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureDto;
import org.example.ortaback.geo.BoundingBox;
import org.example.ortaback.geo.CoordinateValidator;
import org.example.ortaback.geo.GeoJsonConverter;
import org.example.ortaback.geo.PrivacyMasker;
import org.example.ortaback.mapper.EventMapMapper;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.projection.EventClusterProjection;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.service.EventService;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Реализация use-case'ов карты.
 *
 * <p>Главные оптимизации, заложенные в маппинг пачкой:
 * <ol>
 *   <li><b>Приватность за одно обращение к БД</b>: тянем
 *       {@code Set<Long> joinedEventIds} один раз для viewer'а вместо
 *       проверки участия per-event.</li>
 *   <li><b>Batched-counts участников</b> через
 *       {@code countActiveParticipantsByEventIds(ids)} — один SQL на всю
 *       выдачу вместо N.</li>
 * </ol>
 *
 * <p>Известный TODO: lazy-load {@code event.getCategory()} даёт N+1 на этапе
 * маппинга. Решим позже: либо native-проекция с JOIN, либо
 * {@code @EntityGraph} на дополнительной derived-методе репозитория для
 * не-нативных запросов.
 */
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    /**
     * Жёсткий потолок числа фич в одном GeoJSON-ответе — защита фронта от
     * перегруза.
     */
    private static final int GEOJSON_MAX_FEATURES = 5000;

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final CoordinateValidator coordinateValidator;
    private final PrivacyMasker privacyMasker;
    private final GeoProperties geoProperties;
    private final EventMapMapper mapper;

    // -----------------------------------------------------------------------
    // Публичный контракт
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<EventMapMarkerDto> findNearbyEvents(double lat,
                                                    double lng,
                                                    double radiusMeters,
                                                    User viewer) {
        coordinateValidator.validateLatLng(lat, lng);
        coordinateValidator.validateRadiusMeters(radiusMeters);

        List<Event> events = eventRepository.findEventsWithinRadiusOrderedByDistance(
                lat, lng, radiusMeters);
        return toMarkers(events, viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public BoundingBoxResultDto findInBoundingBox(BoundingBox bbox, User viewer) {
        long total = eventRepository.countEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng());

        if (total <= geoProperties.clientClusterThreshold()) {
            List<Event> events = eventRepository.findEventsInBoundingBox(
                    bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng());
            return BoundingBoxResultDto.markers(total, toMarkers(events, viewer));
        }

        int k = clampClusterCount(total);
        List<EventClusterProjection> clusters = eventRepository.clusterEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng(), k);
        return BoundingBoxResultDto.clusters(total, clusters.stream().map(mapper::toCluster).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClusterDto> findClustersInBoundingBox(BoundingBox bbox, Integer zoom) {
        long total = eventRepository.countEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng());

        // TODO: учесть zoom для подстройки числа кластеров; пока — count-based.
        int k = clampClusterCount(total);

        List<EventClusterProjection> clusters = eventRepository.clusterEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng(), k);
        return clusters.stream().map(mapper::toCluster).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GeoJsonFeatureCollectionDto findGeoJsonInBoundingBox(BoundingBox bbox, User viewer) {
        List<Event> events = eventRepository.findEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng());

        if (events.size() > GEOJSON_MAX_FEATURES) {
            events = events.subList(0, GEOJSON_MAX_FEATURES);
        }

        Map<Long, Long> counts = participantCountsForEvents(events);
        Set<Long> joinedEventIds = joinedEventIdsFor(viewer);

        List<GeoJsonFeatureDto> features = events.stream()
                .map(e -> mapper.toFeature(
                        e,
                        visibleLocationFor(e, viewer, joinedEventIds),
                        counts.getOrDefault(e.getId(), 0L)))
                .toList();

        return GeoJsonConverter.toFeatureCollection(features);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Маппит пачку событий в маркеры, разово подтягивая
     * batched-counts участников и список «в чём viewer состоит».
     */
    private List<EventMapMarkerDto> toMarkers(List<Event> events, User viewer) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = participantCountsForEvents(events);
        Set<Long> joinedEventIds = joinedEventIdsFor(viewer);

        return events.stream()
                .map(e -> mapper.toMarker(
                        e,
                        visibleLocationFor(e, viewer, joinedEventIds),
                        counts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    /**
     * Решает, какую точку отдать наружу.
     * <ul>
     *   <li>{@code EXACT} → как есть;</li>
     *   <li>{@code APPROXIMATE} + viewer — создатель события → как есть;</li>
     *   <li>{@code APPROXIMATE} + viewer присоединился → как есть;</li>
     *   <li>иначе → снэппнутая на сетку точка из {@link PrivacyMasker}.</li>
     * </ul>
     */
    private Point visibleLocationFor(Event event, User viewer, Set<Long> joinedEventIds) {
        if (event.getLocationPrecision() == LocationPrecision.EXACT) {
            return event.getLocation();
        }
        if (viewer != null) {
            // FK достаточно — не триггерит lazy-load (Hibernate возвращает id из прокси).
            Long creatorId = event.getCreator().getId();
            if (viewer.getId().equals(creatorId) || joinedEventIds.contains(event.getId())) {
                return event.getLocation();
            }
        }
        return privacyMasker.snap(event.getLocation());
    }

    private Set<Long> joinedEventIdsFor(User viewer) {
        if (viewer == null) {
            return Set.of();
        }
        return new HashSet<>(participantRepository.findEventIdsByUserIdAndStatus(
                viewer.getId(), ParticipantStatus.JOINED));
    }

    /**
     * Возвращает map {@code eventId → activeParticipantCount} одним SQL.
     */
    private Map<Long, Long> participantCountsForEvents(Collection<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : participantRepository.countActiveParticipantsByEventIds(ids)) {
            Long eventId = ((Number) row[0]).longValue();
            Long count   = ((Number) row[1]).longValue();
            result.put(eventId, count);
        }
        return result;
    }

    /**
     * Адаптивно выбирает число кластеров: ~50 событий на кластер, ограничено
     * настройками {@code kmeansMinClusters / kmeansMaxClusters}.
     */
    private int clampClusterCount(long total) {
        GeoProperties.Clustering c = geoProperties.clustering();
        int desired = (int) Math.max(1L, total / 50L);
        return Math.max(c.kmeansMinClusters(), Math.min(c.kmeansMaxClusters(), desired));
    }
}
