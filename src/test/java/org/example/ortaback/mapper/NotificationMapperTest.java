package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.Notification;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    void toDto_withActorAndEvent_mapsAllFields() {
        User actor = User.builder().id(2L).firstName("Aru").lastName("Bek").avatarUrl("a.png").build();
        Category category = Category.builder().id(7L).name("Football").slug("sport").build();
        Event event = Event.builder().id(100L).title("Match").category(category).build();
        Notification n = Notification.builder()
                .id(1L)
                .recipient(User.builder().id(1L).build())
                .type(NotificationType.EVENT_JOIN)
                .actor(actor)
                .event(event)
                .text("Aru Bek присоединился")
                .readAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationDto dto = mapper.toDto(n);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.type()).isEqualTo(NotificationType.EVENT_JOIN);
        assertThat(dto.actorId()).isEqualTo(2L);
        assertThat(dto.actorName()).isEqualTo("Aru Bek");
        assertThat(dto.actorAvatarUrl()).isEqualTo("a.png");
        assertThat(dto.eventId()).isEqualTo(100L);
        assertThat(dto.eventTitle()).isEqualTo("Match");
        assertThat(dto.categorySlug()).isEqualTo("sport");
        assertThat(dto.read()).isFalse();
    }

    @Test
    void toDto_readFlagTrueWhenReadAtSet() {
        Notification n = Notification.builder()
                .id(1L).recipient(User.builder().id(1L).build())
                .type(NotificationType.TRUST_LEVEL_UP)
                .readAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        assertThat(mapper.toDto(n).read()).isTrue();
    }

    @Test
    void toDto_nullActorAndEvent_yieldsNulls() {
        Notification n = Notification.builder()
                .id(1L).recipient(User.builder().id(1L).build())
                .type(NotificationType.TRUST_LEVEL_UP)
                .text("Новый уровень доверия: Свой")
                .createdAt(LocalDateTime.now())
                .build();

        NotificationDto dto = mapper.toDto(n);

        assertThat(dto.actorId()).isNull();
        assertThat(dto.actorName()).isNull();
        assertThat(dto.actorAvatarUrl()).isNull();
        assertThat(dto.eventId()).isNull();
        assertThat(dto.eventTitle()).isNull();
        assertThat(dto.categorySlug()).isNull();
        assertThat(dto.text()).isEqualTo("Новый уровень доверия: Свой");
    }
}
