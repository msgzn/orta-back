package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Строка списка бесед текущего пользователя (ТЗ §4.1): одно событие = одна
 * беседа. Все поля — РЕАЛЬНЫЕ агрегаты (последнее сообщение, непрочитанные,
 * число участников), без заглушек.
 *
 * <p>Беседы без единого сообщения в список не попадают.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversationSummaryDto(
        Long eventId,
        String title,
        String coverImageUrl,
        String lastMessageText,
        LocalDateTime lastMessageAt,
        int unreadCount,
        int participantsCount
) {
}
