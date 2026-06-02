package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.mapper.EventMessageMapper;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventMessage;
import org.example.ortaback.persistence.models.EventMessageRead;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.MessageType;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.repository.EventMessageReadRepository;
import org.example.ortaback.persistence.repository.EventMessageRepository;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация чата события (ТЗ §4.1).
 *
 * <p>Беседа = событие. Доступ — создатель и {@code JOINED}-участники
 * ({@link #requireChatMember}). Непрочитанные считаются от маркера
 * {@link EventMessageRead}: {@code count(id > lastReadMessageId)}; отправка
 * собственного сообщения продвигает маркер, поэтому своё сообщение никогда не
 * числится непрочитанным. Никаких заглушек — все счётчики и превью реальные.
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 30;

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EventMessageRepository messageRepository;
    private final EventMessageReadRepository readRepository;
    private final EventMessageMapper messageMapper;

    @Override
    @Transactional(readOnly = true)
    public PageDto<EventMessageDto> listMessages(Long eventId, Long before, Long after, int size, User viewer) {
        Event event = loadEvent(eventId);
        requireChatMember(event, viewer);
        int pageSize = clampSize(size);

        if (after != null) {
            // Режим опроса: всё новее курсора, по возрастанию — клиент дописывает в конец.
            List<EventMessageDto> content = messageRepository
                    .findByEvent_IdAndIdGreaterThanOrderByIdAsc(eventId, after, PageRequest.of(0, pageSize))
                    .stream()
                    .map(messageMapper::toDto)
                    .toList();
            return new PageDto<>(content, 0, pageSize, content.size(),
                    content.isEmpty() ? 0 : 1, false, false);
        }

        // Скроллбэк / последняя страница: новые сверху.
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<EventMessage> page = (before != null)
                ? messageRepository.findByEvent_IdAndIdLessThan(eventId, before, pageable)
                : messageRepository.findByEvent_Id(eventId, pageable);
        return PageDto.of(page.map(messageMapper::toDto));
    }

    @Override
    @Transactional
    public EventMessageDto sendMessage(Long eventId, String text, User sender) {
        Event event = loadEvent(eventId);
        requireChatMember(event, sender);

        EventMessage msg = messageRepository.save(EventMessage.builder()
                .event(event)
                .sender(sender)
                .type(MessageType.USER)
                .body(text.trim())
                .build());

        // Своё сообщение — сразу прочитано: маркер не должно «догонять».
        upsertRead(event, sender, msg.getId());
        return messageMapper.toDto(msg);
    }

    @Override
    @Transactional
    public void markRead(Long eventId, Long lastMessageId, User user) {
        Event event = loadEvent(eventId);
        requireChatMember(event, user);
        upsertRead(event, user, lastMessageId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> listMyConversations(User user) {
        // Беседы = события, где пользователь JOINED, плюс созданные им.
        Set<Long> eventIds = new LinkedHashSet<>(
                participantRepository.findEventIdsByUserIdAndStatus(user.getId(), ParticipantStatus.JOINED));
        for (Event e : eventRepository.findByCreatorId(user.getId())) {
            eventIds.add(e.getId());
        }
        if (eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Event> events = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));

        List<ConversationSummaryDto> result = new ArrayList<>();
        for (Long eventId : eventIds) {
            Event event = events.get(eventId);
            if (event == null) {
                continue;
            }
            Optional<EventMessage> last = messageRepository.findFirstByEvent_IdOrderByIdDesc(eventId);
            if (last.isEmpty()) {
                continue;   // беседа без сообщений — не показываем
            }
            EventMessage lastMsg = last.get();

            long lastReadId = readRepository.findByEvent_IdAndUser_Id(eventId, user.getId())
                    .map(EventMessageRead::getLastReadMessageId)
                    .orElse(0L);
            long unread = messageRepository.countByEvent_IdAndIdGreaterThan(eventId, lastReadId);
            long participants = participantRepository.countByEventIdAndStatus(eventId, ParticipantStatus.JOINED);

            result.add(new ConversationSummaryDto(
                    eventId,
                    event.getTitle(),
                    event.getCoverImageUrl(),
                    lastMsg.getBody(),
                    lastMsg.getCreatedAt(),
                    (int) unread,
                    (int) participants));
        }
        result.sort(Comparator.comparing(ConversationSummaryDto::lastMessageAt).reversed());
        return result;
    }

    @Override
    @Transactional
    public void postSystemMessage(Event event, String text) {
        messageRepository.save(EventMessage.builder()
                .event(event)
                .sender(null)
                .type(MessageType.SYSTEM)
                .body(text)
                .build());
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private Event loadEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + eventId));
    }

    /** Доступ к беседе: создатель события либо {@code JOINED}-участник. */
    private void requireChatMember(Event event, User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
        User creator = event.getCreator();
        if (creator != null && Objects.equals(creator.getId(), user.getId())) {
            return;
        }
        boolean joined = participantRepository.existsByEventIdAndUserIdAndStatus(
                event.getId(), user.getId(), ParticipantStatus.JOINED);
        if (!joined) {
            throw new AccessDeniedException("You are not a participant of this event's chat");
        }
    }

    /** Монотонно продвигает маркер прочтения; не откатывает назад. */
    private void upsertRead(Event event, User user, Long lastMessageId) {
        if (lastMessageId == null) {
            return;
        }
        EventMessageRead marker = readRepository.findByEvent_IdAndUser_Id(event.getId(), user.getId())
                .orElseGet(() -> EventMessageRead.builder()
                        .event(event)
                        .user(user)
                        .lastReadMessageId(0L)
                        .build());
        if (marker.getLastReadMessageId() == null || lastMessageId > marker.getLastReadMessageId()) {
            marker.setLastReadMessageId(lastMessageId);
            readRepository.save(marker);
        }
    }

    private static int clampSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }
}
