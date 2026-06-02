package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.Notification;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Лента уведомлений получателя, новые сверху. {@code LEFT JOIN FETCH} по
     * to-one связям (actor, event, event.category) безопасен с {@link Pageable}
     * — пагинация остаётся на SQL-стороне (в отличие от fetch коллекций).
     * Это убирает N+1 при маппинге в {@code NotificationDto}.
     */
    @Query(value = """
            SELECT n
              FROM Notification n
              LEFT JOIN FETCH n.actor
              LEFT JOIN FETCH n.event e
              LEFT JOIN FETCH e.category
             WHERE n.recipient.id = :recipientId
             ORDER BY n.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(n)
              FROM Notification n
             WHERE n.recipient.id = :recipientId
            """)
    Page<Notification> findPageByRecipient(@Param("recipientId") Long recipientId, Pageable pageable);

    /** Счётчик непрочитанных — для бейджа на иконке колокольчика. */
    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    /**
     * Пометить одно уведомление прочитанным. Возвращает число затронутых строк
     * (0 — если id чужой/несуществует/уже прочитан): сервис по нему отличает
     * «нечего делать» от успеха, не делая лишний SELECT.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.readAt = :now
             WHERE n.id          = :id
               AND n.recipient.id = :recipientId
               AND n.readAt IS NULL
            """)
    int markRead(@Param("recipientId") Long recipientId,
                 @Param("id") Long id,
                 @Param("now") LocalDateTime now);

    /** Пометить все непрочитанные уведомления получателя. Возвращает число помеченных. */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.readAt = :now
             WHERE n.recipient.id = :recipientId
               AND n.readAt IS NULL
            """)
    int markAllRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);

    /**
     * Идемпотентность напоминаний: проверяем, не отправляли ли уже уведомление
     * данного типа этому получателю по этому событию (используется
     * планировщиком {@code EVENT_REMINDER}, чтобы не слать дубликаты).
     */
    boolean existsByRecipientIdAndEventIdAndType(Long recipientId, Long eventId, NotificationType type);
}
