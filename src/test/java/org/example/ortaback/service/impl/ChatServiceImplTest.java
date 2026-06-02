package org.example.ortaback.service.impl;

import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.mapper.EventMessageMapper;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventMessage;
import org.example.ortaback.persistence.models.EventMessageRead;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.MessageType;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.repository.EventMessageReadRepository;
import org.example.ortaback.persistence.repository.EventMessageRepository;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты {@link ChatServiceImpl} (ТЗ §4.1) на моках — проверяют логику
 * доступа, продвижения маркера прочтения и режимы выборки ленты. Реальный SQL
 * derived-методов прогоняется отдельно в {@code ChatIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock EventMessageRepository messageRepository;
    @Mock EventMessageReadRepository readRepository;
    @Mock EventMessageMapper messageMapper;

    @InjectMocks ChatServiceImpl service;

    private final User creator  = User.builder().id(1L).firstName("Creator").role(UserRole.USER).build();
    private final User outsider = User.builder().id(2L).firstName("Outsider").role(UserRole.USER).build();

    private Event eventOwnedByCreator() {
        return Event.builder().id(100L).creator(creator).title("Match").build();
    }

    private EventMessage rawMessage() {
        return EventMessage.builder().event(eventOwnedByCreator()).type(MessageType.USER).body("x").build();
    }

    private EventMessageDto someDto() {
        return new EventMessageDto(7L, 100L, 1L, "Creator", null,
                MessageType.USER, "x", LocalDateTime.of(2026, 6, 1, 12, 0));
    }

    // -----------------------------------------------------------------------
    //  sendMessage
    // -----------------------------------------------------------------------

    @Test
    void sendMessage_persistsUserMessage_andAdvancesSenderReadMarker() {
        Event event = eventOwnedByCreator();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(messageRepository.save(any(EventMessage.class))).thenAnswer(inv -> {
            EventMessage m = inv.getArgument(0);
            m.setId(5L);
            return m;
        });
        when(readRepository.findByEvent_IdAndUser_Id(100L, 1L)).thenReturn(Optional.empty());
        EventMessageDto expected = someDto();
        when(messageMapper.toDto(any(EventMessage.class))).thenReturn(expected);

        EventMessageDto result = service.sendMessage(100L, "  hello  ", creator);

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<EventMessage> msgCaptor = ArgumentCaptor.forClass(EventMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        EventMessage saved = msgCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(MessageType.USER);
        assertThat(saved.getBody()).isEqualTo("hello");          // trimmed
        assertThat(saved.getSender()).isSameAs(creator);

        ArgumentCaptor<EventMessageRead> readCaptor = ArgumentCaptor.forClass(EventMessageRead.class);
        verify(readRepository).save(readCaptor.capture());
        assertThat(readCaptor.getValue().getLastReadMessageId()).isEqualTo(5L);
    }

    @Test
    void sendMessage_nonMember_throwsAccessDenied() {
        Event event = eventOwnedByCreator();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndUserIdAndStatus(100L, 2L, ParticipantStatus.JOINED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.sendMessage(100L, "hi", outsider))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(messageRepository);
    }

    // -----------------------------------------------------------------------
    //  listMessages
    // -----------------------------------------------------------------------

    @Test
    void listMessages_nonMember_throwsAccessDenied() {
        Event event = eventOwnedByCreator();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndUserIdAndStatus(100L, 2L, ParticipantStatus.JOINED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.listMessages(100L, null, null, 30, outsider))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listMessages_scrollback_usesNewestFirstPaging() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(eventOwnedByCreator()));
        when(messageRepository.findByEvent_Id(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rawMessage(), rawMessage())));
        when(messageMapper.toDto(any(EventMessage.class))).thenReturn(someDto());

        PageDto<EventMessageDto> page = service.listMessages(100L, null, null, 30, creator);

        assertThat(page.content()).hasSize(2);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findByEvent_Id(eq(100L), captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("id");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listMessages_pollMode_returnsAscendingWindow() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(eventOwnedByCreator()));
        when(messageRepository.findByEvent_IdAndIdGreaterThanOrderByIdAsc(eq(100L), eq(5L), any(Pageable.class)))
                .thenReturn(List.of(rawMessage(), rawMessage()));
        when(messageMapper.toDto(any(EventMessage.class))).thenReturn(someDto());

        PageDto<EventMessageDto> page = service.listMessages(100L, null, 5L, 30, creator);

        assertThat(page.content()).hasSize(2);
        assertThat(page.page()).isZero();
        assertThat(page.hasNext()).isFalse();
        verify(messageRepository)
                .findByEvent_IdAndIdGreaterThanOrderByIdAsc(eq(100L), eq(5L), any(Pageable.class));
    }

    // -----------------------------------------------------------------------
    //  markRead
    // -----------------------------------------------------------------------

    @Test
    void markRead_advancesMarkerForward() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(eventOwnedByCreator()));
        when(readRepository.findByEvent_IdAndUser_Id(100L, 1L))
                .thenReturn(Optional.of(EventMessageRead.builder().lastReadMessageId(3L).build()));

        service.markRead(100L, 10L, creator);

        ArgumentCaptor<EventMessageRead> captor = ArgumentCaptor.forClass(EventMessageRead.class);
        verify(readRepository).save(captor.capture());
        assertThat(captor.getValue().getLastReadMessageId()).isEqualTo(10L);
    }

    @Test
    void markRead_lowerId_doesNotRegress() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(eventOwnedByCreator()));
        when(readRepository.findByEvent_IdAndUser_Id(100L, 1L))
                .thenReturn(Optional.of(EventMessageRead.builder().lastReadMessageId(10L).build()));

        service.markRead(100L, 5L, creator);

        verify(readRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    //  postSystemMessage
    // -----------------------------------------------------------------------

    @Test
    void postSystemMessage_savesSystemMessageWithNullSender() {
        Event event = eventOwnedByCreator();

        service.postSystemMessage(event, "Событие началось");

        ArgumentCaptor<EventMessage> captor = ArgumentCaptor.forClass(EventMessage.class);
        verify(messageRepository).save(captor.capture());
        EventMessage saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(MessageType.SYSTEM);
        assertThat(saved.getSender()).isNull();
        assertThat(saved.getBody()).isEqualTo("Событие началось");
    }

    // -----------------------------------------------------------------------
    //  listMyConversations
    // -----------------------------------------------------------------------

    @Test
    void listMyConversations_skipsEmptyConversations_andComputesUnread() {
        Event e100 = Event.builder().id(100L).creator(creator).title("Match A").build();
        Event e200 = Event.builder().id(200L).creator(creator).title("Match B").build();

        when(participantRepository.findEventIdsByUserIdAndStatus(1L, ParticipantStatus.JOINED))
                .thenReturn(List.of(100L, 200L));
        when(eventRepository.findByCreatorId(1L)).thenReturn(List.of());
        when(eventRepository.findAllById(any())).thenReturn(List.of(e100, e200));

        EventMessage last100 = EventMessage.builder()
                .id(9L).event(e100).type(MessageType.USER).body("Latest")
                .createdAt(LocalDateTime.of(2026, 6, 1, 12, 0)).build();
        when(messageRepository.findFirstByEvent_IdOrderByIdDesc(100L)).thenReturn(Optional.of(last100));
        when(messageRepository.findFirstByEvent_IdOrderByIdDesc(200L)).thenReturn(Optional.empty());

        when(readRepository.findByEvent_IdAndUser_Id(100L, 1L))
                .thenReturn(Optional.of(EventMessageRead.builder().lastReadMessageId(2L).build()));
        when(messageRepository.countByEvent_IdAndIdGreaterThan(100L, 2L)).thenReturn(4L);
        when(participantRepository.countByEventIdAndStatus(100L, ParticipantStatus.JOINED)).thenReturn(6L);

        List<ConversationSummaryDto> result = service.listMyConversations(creator);

        assertThat(result).hasSize(1);   // event 200 (без сообщений) пропущено
        ConversationSummaryDto conv = result.get(0);
        assertThat(conv.eventId()).isEqualTo(100L);
        assertThat(conv.lastMessageText()).isEqualTo("Latest");
        assertThat(conv.unreadCount()).isEqualTo(4);
        assertThat(conv.participantsCount()).isEqualTo(6);
    }
}
