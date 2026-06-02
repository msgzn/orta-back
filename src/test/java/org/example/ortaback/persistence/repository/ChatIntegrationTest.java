package org.example.ortaback.persistence.repository;

import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.geo.GeoUtils;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.service.ChatService;
import org.example.ortaback.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест чата события (ТЗ §4.1) поверх РЕАЛЬНОГО PostgreSQL
 * (Testcontainers — см. {@link AbstractIntegrationTest}). Прогоняет
 * {@link ChatService} вместе с derived-запросами {@code EventMessageRepository}
 * /{@code EventMessageReadRepository} и уникальным ключом маркера прочтения
 * против живой БД.
 *
 * <p>Имя класса оканчивается на {@code Test} (не {@code IT}) — иначе surefire
 * (failsafe в pom не подключён) молча пропустил бы тест; та же конвенция, что у
 * {@code EventSearchIntegrationTest}.
 *
 * <p>{@code @Transactional} → откат после каждого метода. Беседа собирается
 * напрямую через репозитории (без {@code EventService.joinEvent}), поэтому в
 * ленте только пользовательские сообщения — без системных.
 */
@Transactional
class ChatIntegrationTest extends AbstractIntegrationTest {

    @Autowired ChatService chatService;
    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired EventParticipantRepository participantRepository;

    private static final double LAT = 43.238;
    private static final double LNG = 76.945;

    private User creatorA;
    private User memberB;
    private User outsiderC;
    private Long eventId;

    private User user(String tag) {
        return User.builder()
                .firstName(tag).lastName("User")
                .email(tag + "-" + System.nanoTime() + "@example.com")
                .hashedPassword("BCRYPT")
                .trustScore(40.0f)
                .build();
    }

    @BeforeEach
    void seed() {
        Category category = categoryRepository.saveAndFlush(
                Category.builder().name("Chat-" + System.nanoTime()).build());

        creatorA = userRepository.saveAndFlush(user("a"));
        memberB = userRepository.saveAndFlush(user("b"));
        outsiderC = userRepository.saveAndFlush(user("c"));

        Event event = eventRepository.saveAndFlush(Event.builder()
                .creator(creatorA)
                .category(category)
                .status(EventStatus.ACTIVE)
                .title("Chat Event")
                .description("desc")
                .location(GeoUtils.point(LAT, LNG))
                .locationPrecision(LocationPrecision.EXACT)
                .startTime(LocalDateTime.of(2026, 7, 1, 18, 0))
                .maxParticipants(10)
                .build());
        eventId = event.getId();

        participantRepository.saveAndFlush(EventParticipant.builder()
                .event(event).user(creatorA).status(ParticipantStatus.JOINED).build());
        participantRepository.saveAndFlush(EventParticipant.builder()
                .event(event).user(memberB).status(ParticipantStatus.JOINED).build());
    }

    @Test
    void sendAndScrollback_returnsNewestFirst() {
        chatService.sendMessage(eventId, "m1", creatorA);
        chatService.sendMessage(eventId, "m2", memberB);
        chatService.sendMessage(eventId, "m3", creatorA);

        PageDto<EventMessageDto> page = chatService.listMessages(eventId, null, null, 30, creatorA);

        assertThat(page.content()).extracting(EventMessageDto::text)
                .containsExactly("m3", "m2", "m1");
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void pollMode_returnsAscendingAfterCursor() {
        EventMessageDto m1 = chatService.sendMessage(eventId, "m1", creatorA);
        chatService.sendMessage(eventId, "m2", creatorA);
        chatService.sendMessage(eventId, "m3", creatorA);

        PageDto<EventMessageDto> poll = chatService.listMessages(eventId, null, m1.id(), 30, memberB);

        assertThat(poll.content()).extracting(EventMessageDto::text)
                .containsExactly("m2", "m3");
    }

    @Test
    void conversationsUnreadThenMarkReadZero() {
        chatService.sendMessage(eventId, "m1", creatorA);
        chatService.sendMessage(eventId, "m2", creatorA);
        EventMessageDto m3 = chatService.sendMessage(eventId, "m3", creatorA);

        List<ConversationSummaryDto> before = chatService.listMyConversations(memberB);
        assertThat(before).hasSize(1);
        assertThat(before.get(0).eventId()).isEqualTo(eventId);
        assertThat(before.get(0).unreadCount()).isEqualTo(3);
        assertThat(before.get(0).participantsCount()).isEqualTo(2);
        assertThat(before.get(0).lastMessageText()).isEqualTo("m3");

        chatService.markRead(eventId, m3.id(), memberB);

        assertThat(chatService.listMyConversations(memberB).get(0).unreadCount()).isZero();
    }

    @Test
    void nonMember_cannotSend() {
        assertThatThrownBy(() -> chatService.sendMessage(eventId, "x", outsiderC))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonMember_cannotList() {
        assertThatThrownBy(() -> chatService.listMessages(eventId, null, null, 30, outsiderC))
                .isInstanceOf(AccessDeniedException.class);
    }
}
