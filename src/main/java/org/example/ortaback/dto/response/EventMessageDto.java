package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.MessageType;

import java.time.LocalDateTime;

/**
 * Сообщение чата события (ТЗ §4.1) для отдачи клиенту.
 *
 * <p>Для системных сообщений ({@link MessageType#SYSTEM}) {@code senderId},
 * {@code senderName}, {@code senderAvatarUrl} равны {@code null} и опускаются
 * из JSON ({@link JsonInclude}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMessageDto(
        Long id,
        Long eventId,
        Long senderId,
        String senderName,
        String senderAvatarUrl,
        MessageType type,
        String text,
        LocalDateTime createdAt
) {
}
