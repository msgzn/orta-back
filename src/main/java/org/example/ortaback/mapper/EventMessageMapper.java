package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.persistence.models.EventMessage;
import org.example.ortaback.persistence.models.User;
import org.springframework.stereotype.Component;

/**
 * Маппер {@link EventMessage} → {@link EventMessageDto}. Рассчитан на то, что
 * вызывающий репозиторий уже сделал fetch автора через
 * {@code @EntityGraph(attributePaths = "sender")} — сам LAZY-связь не
 * инициализирует. Для системных сообщений {@code sender == null}.
 */
@Component
public class EventMessageMapper {

    public EventMessageDto toDto(EventMessage m) {
        User sender = m.getSender();
        Long senderId = sender != null ? sender.getId() : null;
        String senderName = sender != null ? fullName(sender) : null;
        String senderAvatarUrl = sender != null ? sender.getAvatarUrl() : null;

        return new EventMessageDto(
                m.getId(),
                m.getEvent().getId(),
                senderId,
                senderName,
                senderAvatarUrl,
                m.getType(),
                m.getBody(),
                m.getCreatedAt()
        );
    }

    private String fullName(User u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
