package org.example.ortaback.service.impl;

import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UnreadCountDto;
import org.example.ortaback.mapper.NotificationMapper;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.Notification;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.example.ortaback.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;

    @InjectMocks NotificationServiceImpl service;

    @Test
    void listMyNotifications_wrapsPageAndMaps() {
        Notification n = Notification.builder().id(5L).recipient(User.builder().id(1L).build())
                .type(NotificationType.EVENT_JOIN).createdAt(LocalDateTime.now()).build();
        Page<Notification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(notificationRepository.findPageByRecipient(eq(1L), any(Pageable.class))).thenReturn(page);
        NotificationDto dto = new NotificationDto(5L, NotificationType.EVENT_JOIN,
                null, null, null, null, null, null, null, false, n.getCreatedAt());
        when(notificationMapper.toDto(n)).thenReturn(dto);

        PageDto<NotificationDto> result = service.listMyNotifications(1L, 0, 20);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(3L);

        UnreadCountDto dto = service.getUnreadCount(1L);

        assertThat(dto.count()).isEqualTo(3L);
    }

    @Test
    void markRead_delegatesWithRecipientGuard() {
        service.markRead(1L, 5L);

        verify(notificationRepository).markRead(eq(1L), eq(5L), any(LocalDateTime.class));
    }

    @Test
    void markAllRead_delegates() {
        service.markAllRead(1L);

        verify(notificationRepository).markAllRead(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void create_buildsAndSavesNotification() {
        User recipient = User.builder().id(1L).build();
        User actor = User.builder().id(2L).build();
        Event event = Event.builder().id(100L).build();

        service.create(recipient, NotificationType.EVENT_JOIN, actor, event, "joined");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isEqualTo(recipient);
        assertThat(saved.getType()).isEqualTo(NotificationType.EVENT_JOIN);
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getEvent()).isEqualTo(event);
        assertThat(saved.getText()).isEqualTo("joined");
        assertThat(saved.getReadAt()).isNull();
    }
}
