package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.Notification;
import org.example.ortaback.persistence.models.User;
import org.springframework.stereotype.Component;

/**
 * Маппер {@link Notification} → {@link NotificationDto}. Рассчитан на то, что
 * вызывающий репозиторий уже сделал fetch-join по actor/event/category
 * (см. {@code NotificationRepository.findPageByRecipient}); сам он LAZY-связи
 * не инициализирует.
 */
@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification n) {
        User actor = n.getActor();
        Event event = n.getEvent();

        Long actorId = actor != null ? actor.getId() : null;
        String actorName = actor != null ? fullName(actor) : null;
        String actorAvatarUrl = actor != null ? actor.getAvatarUrl() : null;

        Long eventId = event != null ? event.getId() : null;
        String eventTitle = event != null ? event.getTitle() : null;
        String categorySlug = (event != null && event.getCategory() != null)
                ? event.getCategory().getSlug()
                : null;

        return new NotificationDto(
                n.getId(),
                n.getType(),
                actorId,
                actorName,
                actorAvatarUrl,
                eventId,
                eventTitle,
                categorySlug,
                n.getText(),
                n.getReadAt() != null,
                n.getCreatedAt()
        );
    }

    private String fullName(User u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
