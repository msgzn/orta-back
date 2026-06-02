package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Элемент ленты уведомлений (ТЗ §4.2). Плоский DTO: вместо вложенных
 * actor/event объектов — только те поля, что нужны карточке (имя+аватар
 * инициатора, заголовок события, slug категории для иконки/цвета). Поля
 * {@code actorId}/{@code eventId} дают фронту deep-link по тапу.
 *
 * <p>{@code read} выводится из {@code Notification.readAt != null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationDto(
        Long id,
        NotificationType type,
        Long actorId,
        String actorName,
        String actorAvatarUrl,
        Long eventId,
        String eventTitle,
        String categorySlug,
        String text,
        boolean read,
        LocalDateTime createdAt
) {}
