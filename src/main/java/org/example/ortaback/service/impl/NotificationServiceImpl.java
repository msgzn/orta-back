package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UnreadCountDto;
import org.example.ortaback.mapper.NotificationMapper;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.Notification;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.example.ortaback.persistence.repository.NotificationRepository;
import org.example.ortaback.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageDto<NotificationDto> listMyNotifications(Long recipientId, int page, int size) {
        return PageDto.of(
                notificationRepository.findPageByRecipient(recipientId, PageRequest.of(page, size))
                        .map(notificationMapper::toDto)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountDto getUnreadCount(Long recipientId) {
        return new UnreadCountDto(notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId));
    }

    @Override
    @Transactional
    public void markRead(Long recipientId, Long notificationId) {
        // Возвращаемое число строк игнорируем намеренно: и «отметили», и «нечего
        // отмечать» (чужой/несуществующий/уже прочитан id) для клиента — успех (204).
        notificationRepository.markRead(recipientId, notificationId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllRead(recipientId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void create(User recipient, NotificationType type, User actor, Event event, String text) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .actor(actor)
                .event(event)
                .text(text)
                .build();
        notificationRepository.save(notification);
    }
}
