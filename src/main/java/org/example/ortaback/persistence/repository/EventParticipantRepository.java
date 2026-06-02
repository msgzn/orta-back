package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEventId(Long eventId);

    List<EventParticipant> findByUserId(Long userId);

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, ParticipantStatus status);

    long countByEventIdAndStatus(Long eventId, ParticipantStatus status);

    /** Сколько событий, где пользователь сейчас числится с заданным статусом (агрегаты профиля, ТЗ §4.5). */
    long countByUserIdAndStatus(Long userId, ParticipantStatus status);

    /**
     * Список id событий, в которых пользователь сейчас числится с заданным
     * статусом. Используется приватной маскировкой координат: креатор и
     * присоединившиеся к событию видят точную локацию даже у APPROXIMATE
     * мероприятий.
     */
    @Query("""
            SELECT ep.event.id
              FROM EventParticipant ep
             WHERE ep.user.id = :userId
               AND ep.status  = :status
            """)
    List<Long> findEventIdsByUserIdAndStatus(@Param("userId") Long userId,
                                             @Param("status") ParticipantStatus status);

    /**
     * Пользователи, числящиеся в событии с заданным статусом. Используется
     * генерацией уведомлений (ТЗ §4.2): RATE_PROMPT по завершении и
     * EVENT_REMINDER из планировщика рассылаются всем {@code JOINED}.
     */
    @Query("""
            SELECT ep.user
              FROM EventParticipant ep
             WHERE ep.event.id = :eventId
               AND ep.status   = :status
            """)
    List<User> findUsersByEventIdAndStatus(@Param("eventId") Long eventId,
                                           @Param("status") ParticipantStatus status);

    /**
     * Batched-подсчёт активных участников для пачки событий — одним SQL
     * вместо N (по одному запросу на маркер карты).
     *
     * <p>Каждая строка — это {@code [eventId, count]}, оба значения возвращаются
     * как {@code Long} (PostgreSQL: {@code bigint}).
     */
    @Query(value = """
            SELECT ep.event_id, COUNT(ep.id)
              FROM event_participant ep
             WHERE ep.event_id IN (:eventIds)
               AND ep.status   = 'JOINED'
             GROUP BY ep.event_id
            """, nativeQuery = true)
    List<Object[]> countActiveParticipantsByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
