package org.example.ortaback.mapper;

import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureDto;
import org.example.ortaback.dto.response.CreatorMiniDto;
import org.example.ortaback.dto.response.EventFeedItemDto;
import org.example.ortaback.geo.GeoJsonConverter;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.projection.EventClusterProjection;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Маппинг доменных сущностей в DTO карты.
 *
 * <p>Не используем MapStruct: при маппинге нужно подмешивать
 * приватно-маскированную точку (она не лежит в сущности) и собирать
 * {@code properties} для GeoJSON Feature — MapStruct тут не помогает,
 * а размывает контракт.
 *
 * <p>NB: обращения к {@code event.getCategory()} / {@code event.getCreator()}
 * триггерят lazy-load. Для пачки событий это даёт N+1 — оптимизация
 * (через {@code @EntityGraph} или native-проекцию с JOIN) планируется
 * на этапе перформанс-полировки.
 */
@Component
public class EventMapMapper {

    public EventMapMarkerDto toMarker(Event event, Point visibleLocation, long participantsCount) {
        Category category = event.getCategory();
        return new EventMapMarkerDto(
                event.getId(),
                event.getTitle(),
                visibleLocation.getY(),
                visibleLocation.getX(),
                category != null ? category.getIconUrl() : null,
                participantsCount,
                event.getLocationPrecision()
        );
    }

    public EventDetailDto toDetail(Event event, Point visibleLocation, long participantsCount) {
        Category category = event.getCategory();
        User creator = event.getCreator();
        return new EventDetailDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getPlaceName(),
                event.getAddress(),
                visibleLocation.getY(),
                visibleLocation.getX(),
                event.getStartTime(),
                event.getTimeDuration(),
                event.getMinParticipants(),
                event.getMaxParticipants(),
                event.getPricePerPerson(),
                event.getStatus(),
                event.getLocationPrecision(),
                category != null ? category.getId()      : null,
                category != null ? category.getName()    : null,
                category != null ? category.getIconUrl() : null,
                creator.getId(),
                creator.getFirstName(),
                creator.getLastName(),
                creator.getAvatarUrl(),
                participantsCount,
                event.getCoverImageUrl()
        );
    }

    public GeoJsonFeatureDto toFeature(Event event, Point visibleLocation, long participantsCount) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", event.getTitle());
        properties.put("status", event.getStatus().name());
        properties.put("startTime", event.getStartTime());
        properties.put("locationPrecision", event.getLocationPrecision().name());
        properties.put("participantsCount", participantsCount);
        if (event.getCategory() != null) {
            properties.put("categoryId", event.getCategory().getId());
            properties.put("categoryIcon", event.getCategory().getIconUrl());
        }
        return GeoJsonConverter.toFeature(event.getId(), visibleLocation, properties);
    }

    public EventFeedItemDto toFeedItem(Event event, Point visibleLocation, long participantsCount) {
        Category category = event.getCategory();
        User creator = event.getCreator();

        CreatorMiniDto creatorMini = new CreatorMiniDto(
                creator.getId(),
                creator.getFirstName(),
                creator.getLastName(),
                creator.getAvatarUrl(),
                creator.getTrustScore()
        );

        return new EventFeedItemDto(
                event.getId(),
                event.getTitle(),
                event.getPlaceName(),
                event.getAddress(),
                visibleLocation != null ? visibleLocation.getY() : null,
                visibleLocation != null ? visibleLocation.getX() : null,
                event.getLocationPrecision(),
                event.getStartTime(),
                event.getTimeDuration(),
                event.getMaxParticipants(),
                participantsCount,
                event.getPricePerPerson(),
                event.getStatus(),
                category != null ? category.getId()      : null,
                category != null ? category.getName()    : null,
                category != null ? category.getIconUrl() : null,
                creatorMini,
                event.getCoverImageUrl()
        );
    }

    public ClusterDto toCluster(EventClusterProjection projection) {
        return new ClusterDto(
                projection.getClusterId(),
                projection.getEventCount() != null ? projection.getEventCount() : 0L,
                projection.getCenterLat(),
                projection.getCenterLng()
        );
    }
}
