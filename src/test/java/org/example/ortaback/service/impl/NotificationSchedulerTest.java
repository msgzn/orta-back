package org.example.ortaback.service.impl;

import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.persistence.repository.NotificationRepository;
import org.example.ortaback.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationService notificationService;

    @InjectMocks NotificationScheduler scheduler;

    @Test
    void sendEventReminders_fansOutToJoined_andSkipsAlreadyReminded() {
        User creator = User.builder().id(1L).firstName("Creator").build();
        User other   = User.builder().id(2L).firstName("Other").build();
        Event event  = Event.builder().id(100L).creator(creator).title("Match")
                .startTime(LocalDateTime.now().plusMinutes(30)).build();

        when(eventRepository.findActiveStartingBetween(any(), any())).thenReturn(List.of(event));
        when(participantRepository.findUsersByEventIdAndStatus(100L, ParticipantStatus.JOINED))
                .thenReturn(List.of(other));
        when(notificationRepository.existsByRecipientIdAndEventIdAndType(2L, 100L, NotificationType.EVENT_REMINDER))
                .thenReturn(false);
        when(notificationRepository.existsByRecipientIdAndEventIdAndType(1L, 100L, NotificationType.EVENT_REMINDER))
                .thenReturn(true);   // креатору уже напоминали — дубль не шлём

        scheduler.sendEventReminders();

        verify(notificationService).create(
                eq(other), eq(NotificationType.EVENT_REMINDER), isNull(), eq(event), any());
        verify(notificationService, never()).create(
                eq(creator), any(), any(), any(), any());
    }

    @Test
    void sendEventReminders_noEventsSoon_isNoop() {
        when(eventRepository.findActiveStartingBetween(any(), any())).thenReturn(List.of());

        scheduler.sendEventReminders();

        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }
}
