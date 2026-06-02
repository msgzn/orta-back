package org.example.ortaback.service;

import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UnreadCountDto;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;

/**
 * In-app уведомления (ТЗ §4.2): лента, счётчик непрочитанных, отметка о
 * прочтении и серверная генерация уведомлений из бизнес-хуков.
 */
public interface NotificationService {

    // ---------------- Чтение (вызывается контроллером) -------------------

    /** Лента уведомлений текущего пользователя, новые сверху. */
    PageDto<NotificationDto> listMyNotifications(Long recipientId, int page, int size);

    /** Число непрочитанных — для бейджа. */
    UnreadCountDto getUnreadCount(Long recipientId);

    /** Отметить одно уведомление прочитанным (no-op, если оно чужое/уже прочитано). */
    void markRead(Long recipientId, Long notificationId);

    /** Отметить все уведомления получателя прочитанными. */
    void markAllRead(Long recipientId);

    // ---------------- Генерация (вызывается из бизнес-хуков) -------------

    /**
     * Создать уведомление. Метод участвует в транзакции вызывающего хука
     * (см. {@code @Transactional} на реализации с {@code Propagation.MANDATORY}
     * либо обычное присоединение), поэтому уведомление откатится вместе с
     * бизнес-операцией, если та упадёт.
     *
     * <p>{@code actor}/{@code event} опциональны. Только in-app запись; push/FCM
     * — отдельный канал и здесь не затрагивается.
     */
    void create(User recipient, NotificationType type, User actor, Event event, String text);
}
