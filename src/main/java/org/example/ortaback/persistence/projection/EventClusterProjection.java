package org.example.ortaback.persistence.projection;

import org.springframework.beans.factory.annotation.Value;

/**
 * Проекция результата серверной кластеризации событий
 * ({@code EventRepository.clusterEventsInBoundingBox}).
 *
 * <p>Используется при увеличении количества точек в видимой области карты выше
 * порога {@code application.geo.client-cluster-threshold} — вместо отдачи
 * отдельных маркеров сервер схлопывает их в кластеры через
 * {@code ST_ClusterKMeans} и возвращает только центроиды.
 *
 * <p>Применяется open-projection через SpEL ({@code @Value("#{target.…}")}) —
 * это надёжный способ маппить native-query на интерфейс, не привязываясь к
 * case-folding и неоднозначностям column-label vs property-name в JPA.
 */
public interface EventClusterProjection {

    @Value("#{target.cluster_id}")
    Integer getClusterId();

    @Value("#{target.event_count}")
    Long getEventCount();

    @Value("#{target.center_lng}")
    Double getCenterLng();

    @Value("#{target.center_lat}")
    Double getCenterLat();
}
