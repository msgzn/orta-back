package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureDto;
import org.example.ortaback.dto.request.EventCreateRequestDto;
import org.example.ortaback.dto.request.EventUpdateRequestDto;
import org.example.ortaback.dto.response.ParticipantResponseDto;
import org.example.ortaback.exception.EventStateException;
import org.example.ortaback.exception.InvalidCoordinatesException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.geo.BoundingBox;
import org.example.ortaback.geo.CoordinateValidator;
import org.example.ortaback.geo.GeoJsonConverter;
import org.example.ortaback.geo.GeoUtils;
import org.example.ortaback.geo.PrivacyMasker;
import org.example.ortaback.mapper.EventMapMapper;
import org.example.ortaback.mapper.ParticipantMapper;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.projection.EventClusterProjection;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.service.EventService;
import org.locationtech.jts.geom.Point;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int GEOJSON_MAX_FEATURES = 5000;

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final CategoryRepository categoryRepository;
    private final CoordinateValidator coordinateValidator;
    private final PrivacyMasker privacyMasker;
    private final GeoProperties geoProperties;
    private final EventMapMapper mapper;
    private final ParticipantMapper participantMapper;

    // =======================================================================
    //  Geo / map
    // =======================================================================

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
        return BoundingBoxResultDto.clusters(total,
                clusters.stream().map(mapper::toCluster).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClusterDto> findClustersInBoundingBox(BoundingBox bbox, Integer zoom) {
        long total = eventRepository.countEventsInBoundingBox(
                bbox.minLat(), bbox.minLng(), bbox.maxLat(), bbox.maxLng());

        // TODO: учесть zoom при выборе числа кластеров.
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

    // =======================================================================
    //  CRUD
    // =======================================================================

    @Override
    @Transactional
    public EventDetailDto createEvent(EventCreateRequestDto request, User creator) {
        coordinateValidator.validateLatLng(request.lat(), request.lng());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: id=" + request.categoryId()));

        if (request.minParticipants() != null
                && request.maxParticipants() != null
                && request.minParticipants() > request.maxParticipants()) {
            throw new EventStateException("minParticipants must be ≤ maxParticipants");
        }

        Event event = Event.builder()
                .creator(creator)
                .category(category)
                .status(EventStatus.ACTIVE)
                .title(request.title())
                .description(request.description())
                .placeName(request.placeName())
                .address(request.address())
                .location(GeoUtils.point(request.lat(), request.lng()))
                .locationPrecision(request.locationPrecision() != null
                        ? request.locationPrecision()
                        : LocationPrecision.EXACT)
                .startTime(request.startTime())
                .timeDuration(request.timeDuration())
                .minParticipants(request.minParticipants())
                .maxParticipants(request.maxParticipants())
                .pricePerPerson(request.pricePerPerson())
                .build();

        event = eventRepository.save(event);

        // Creator автоматически в составе.
        participantRepository.save(EventParticipant.builder()
                .event(event)
                .user(creator)
                .status(ParticipantStatus.JOINED)
                .build());

        return mapper.toDetail(event, event.getLocation(), 1L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEvent(Long id, User viewer) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + id));

        Set<Long> joinedEventIds = joinedEventIdsFor(viewer);
        Point visibleLocation = visibleLocationFor(event, viewer, joinedEventIds);

        long count = participantRepository.countByEventIdAndStatus(id, ParticipantStatus.JOINED);
        return mapper.toDetail(event, visibleLocation, count);
    }

    @Override
    @Transactional
    public EventDetailDto updateEvent(Long id, EventUpdateRequestDto request, User actor) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + id));
        requireCreatorOrAdmin(event, actor);

        if (request.title()       != null) event.setTitle(request.title());
        if (request.description() != null) event.setDescription(request.description());
        if (request.placeName()   != null) event.setPlaceName(request.placeName());
        if (request.address()     != null) event.setAddress(request.address());

        // Координаты: либо обе, либо ни одной.
        if (request.lat() != null || request.lng() != null) {
            if (request.lat() == null || request.lng() == null) {
                throw new InvalidCoordinatesException(
                        "lat and lng must be provided together");
            }
            coordinateValidator.validateLatLng(request.lat(), request.lng());
            event.setLocation(GeoUtils.point(request.lat(), request.lng()));
        }

        if (request.locationPrecision() != null) event.setLocationPrecision(request.locationPrecision());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found: id=" + request.categoryId()));
            event.setCategory(category);
        }

        if (request.startTime()       != null) event.setStartTime(request.startTime());
        if (request.timeDuration()    != null) event.setTimeDuration(request.timeDuration());
        if (request.minParticipants() != null) event.setMinParticipants(request.minParticipants());
        if (request.maxParticipants() != null) event.setMaxParticipants(request.maxParticipants());
        if (request.pricePerPerson()  != null) event.setPricePerPerson(request.pricePerPerson());
        if (request.status()          != null) event.setStatus(request.status());

        if (event.getMinParticipants() != null
                && event.getMaxParticipants() != null
                && event.getMinParticipants() > event.getMaxParticipants()) {
            throw new EventStateException("minParticipants must be ≤ maxParticipants");
        }

        // Dirty checking — entity managed, изменения улетят на commit.

        long count = participantRepository.countByEventIdAndStatus(id, ParticipantStatus.JOINED);
        // Креатор/admin — всегда видит реальные координаты.
        return mapper.toDetail(event, event.getLocation(), count);
    }

    @Override
    @Transactional
    public void cancelEvent(Long id, User actor) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + id));
        requireCreatorOrAdmin(event, actor);

        if (event.getStatus() == EventStatus.CANCELED) {
            // идемпотентно, без шума
            return;
        }
        event.setStatus(EventStatus.CANCELED);
    }

    // =======================================================================
    //  Участие
    // =======================================================================

    @Override
    @Transactional
    public void joinEvent(Long eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + eventId));

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new EventStateException(
                    "Cannot join an event with status " + event.getStatus());
        }

        Optional<EventParticipant> existing = participantRepository.findByEventIdAndUserId(
                eventId, user.getId());

        if (existing.isPresent()) {
            EventParticipant p = existing.get();
            switch (p.getStatus()) {
                case JOINED -> throw new ResourceAlreadyExistsException(
                        "Already joined this event");
                case KICKED -> throw new EventStateException(
                        "You were removed from this event and cannot rejoin");
                case LEFT -> {
                    enforceCapacity(event);
                    p.setStatus(ParticipantStatus.JOINED);
                }
            }
            return;
        }

        enforceCapacity(event);
        participantRepository.save(EventParticipant.builder()
                .event(event)
                .user(user)
                .status(ParticipantStatus.JOINED)
                .build());
    }

    @Override
    @Transactional
    public void leaveEvent(Long eventId, User user) {
        EventParticipant p = participantRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Not a participant of event id=" + eventId));

        if (Objects.equals(p.getEvent().getCreator().getId(), user.getId())) {
            throw new EventStateException(
                    "The creator cannot leave their own event — cancel it instead");
        }
        if (p.getStatus() != ParticipantStatus.JOINED) {
            // ничего не меняем
            return;
        }
        p.setStatus(ParticipantStatus.LEFT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponseDto> listParticipants(Long eventId, User viewer) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found: id=" + eventId);
        }
        return participantRepository.findByEventId(eventId).stream()
                .map(participantMapper::toDto)
                .toList();
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

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

    private Point visibleLocationFor(Event event, User viewer, Set<Long> joinedEventIds) {
        if (event.getLocationPrecision() == LocationPrecision.EXACT) {
            return event.getLocation();
        }
        if (viewer != null) {
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

    private Map<Long, Long> participantCountsForEvents(Collection<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : participantRepository.countActiveParticipantsByEventIds(ids)) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private void enforceCapacity(Event event) {
        if (event.getMaxParticipants() == null) {
            return;
        }
        long joined = participantRepository.countByEventIdAndStatus(
                event.getId(), ParticipantStatus.JOINED);
        if (joined >= event.getMaxParticipants()) {
            throw new EventStateException("Event is full");
        }
    }

    private static void requireCreatorOrAdmin(Event event, User actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authentication required");
        }
        boolean isCreator = Objects.equals(event.getCreator().getId(), actor.getId());
        boolean isAdmin = actor.getRole() == UserRole.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new AccessDeniedException(
                    "Only the event creator or an admin can perform this action");
        }
    }

    private int clampClusterCount(long total) {
        GeoProperties.Clustering c = geoProperties.clustering();
        int desired = (int) Math.max(1L, total / 50L);
        return Math.max(c.kmeansMinClusters(), Math.min(c.kmeansMaxClusters(), desired));
    }
}
