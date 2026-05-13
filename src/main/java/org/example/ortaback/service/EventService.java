package org.example.ortaback.service;

import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.dto.request.EventCreateRequestDto;
import org.example.ortaback.dto.request.EventUpdateRequestDto;
import org.example.ortaback.dto.response.EventFeedItemDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.ParticipantResponseDto;
import org.example.ortaback.geo.BoundingBox;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Use-case'ы событий: и геопространственные (карта), и доменный CRUD,
 * и участие пользователей.
 */
public interface EventService {

    // ---------------- Геопространственные ---------------------------------

    List<EventMapMarkerDto> findNearbyEvents(double lat,
                                             double lng,
                                             double radiusMeters,
                                             User viewer);

    BoundingBoxResultDto findInBoundingBox(BoundingBox bbox, User viewer);

    List<ClusterDto> findClustersInBoundingBox(BoundingBox bbox, Integer zoom);

    GeoJsonFeatureCollectionDto findGeoJsonInBoundingBox(BoundingBox bbox, User viewer);

    // ---------------- CRUD ------------------------------------------------

    /**
     * Создаёт событие; creator автоматически становится участником
     * со статусом JOINED.
     */
    EventDetailDto createEvent(EventCreateRequestDto request, User creator);

    /**
     * Полная карточка события с учётом приватной маскировки координат для
     * viewer'а.
     */
    EventDetailDto getEvent(Long id, User viewer);

    /**
     * PATCH-обновление: только переданные ({@code != null}) поля изменяются.
     * Только creator или ADMIN.
     */
    EventDetailDto updateEvent(Long id, EventUpdateRequestDto request, User actor);

    /**
     * Soft-cancel: ставит {@code status = CANCELED}. Только creator или ADMIN.
     */
    void cancelEvent(Long id, User actor);

    // ---------------- Участие --------------------------------------------

    void joinEvent(Long eventId, User user);

    void leaveEvent(Long eventId, User user);

    List<ParticipantResponseDto> listParticipants(Long eventId, User viewer);

    // ---------------- Лента / feed ---------------------------------------

    /**
     * Стандартная лента: ACTIVE-события опционально отфильтрованные по
     * категории, отсортированные по {@code startTime asc}.
     */
    PageDto<EventFeedItemDto> findFeed(Long categoryId, int page, int size, User viewer);

    /**
     * Персонализированная лента через pgvector cosine-distance к
     * {@code user.global_interest_embedding}. Если у пользователя нет
     * эмбеддинга — прозрачно фоллбэчит на {@link #findFeed(Long, int, int, User)}
     * (sort=recent).
     */
    PageDto<EventFeedItemDto> findRecommended(Long categoryId, int page, int size, User viewer);

    // ---------------- Lifecycle: start / finish ---------------------------

    /**
     * Промоутер запускает событие: ставится {@code started_at = now}, шлётся
     * push всем JOINED-участникам (placeholder, пока FCM не подключён).
     * Только creator или ADMIN.
     */
    void startEvent(Long eventId, User actor);

    /**
     * Событие закончилось: {@code status = DONE}. Открывается окно отзывов
     * (rating system — см. отдельную итерацию). Только creator или ADMIN.
     */
    void finishEvent(Long eventId, User actor);

    // ---------------- Observe ---------------------------------------------

    /**
     * Подписаться на событие — попадает в «Observed» в профиле и получит
     * push «There are enough members» при достижении {@code minParticipants}.
     */
    void observeEvent(Long eventId, User user);

    void unobserveEvent(Long eventId, User user);

    // ---------------- Cover image -----------------------------------------

    /** Загружает обложку события в S3. Только creator или ADMIN. */
    String uploadCover(Long eventId, MultipartFile file, User actor);

    /** Удаляет обложку события из S3 (best-effort). */
    void deleteCover(Long eventId, User actor);

    // ---------------- «My events» tabs в профиле --------------------------

    PageDto<EventFeedItemDto> listMyCreatedEvents(User user, EventStatus status, int page, int size);

    PageDto<EventFeedItemDto> listMyJoinedEvents(User user, Boolean upcoming, int page, int size);

    PageDto<EventFeedItemDto> listMyObservedEvents(User user, int page, int size);
}
