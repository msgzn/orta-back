package org.example.ortaback.service;

import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.User;

import java.util.List;

/**
 * Чат события (ТЗ §4.1). Беседа-контейнер = событие; участники беседы —
 * создатель и {@code JOINED}-участники. Доступ к ленте/отправке имеют только
 * они (иначе {@link org.springframework.security.access.AccessDeniedException}).
 *
 * <p>{@link #postSystemMessage} вызывается из {@code EventService} на
 * join/leave/start/finish/cancel и пишет служебное сообщение
 * ({@code sender == null}) в ту же транзакцию бизнес-операции.
 */
public interface ChatService {

    /**
     * Лента сообщений беседы.
     *
     * <ul>
     *   <li>{@code after != null} — режим опроса: сообщения новее курсора
     *       {@code after}, по возрастанию id;</li>
     *   <li>{@code before != null} — скроллбэк: сообщения старше курсора, новые
     *       сверху;</li>
     *   <li>оба {@code null} — последняя страница, новые сверху.</li>
     * </ul>
     */
    PageDto<EventMessageDto> listMessages(Long eventId, Long before, Long after, int size, User viewer);

    /** Отправить пользовательское сообщение; продвигает маркер прочтения автора. */
    EventMessageDto sendMessage(Long eventId, String text, User sender);

    /** Отметить прочитанным до {@code lastMessageId} включительно (монотонно). */
    void markRead(Long eventId, Long lastMessageId, User user);

    /** Список бесед пользователя (события, где он создатель/{@code JOINED}), с непрочитанными. */
    List<ConversationSummaryDto> listMyConversations(User user);

    /** Системное сообщение беседы (join/leave/start/finish/cancel), {@code sender == null}. */
    void postSystemMessage(Event event, String text);
}
