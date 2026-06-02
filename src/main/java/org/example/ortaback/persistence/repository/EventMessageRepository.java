package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.EventMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Сообщения чата события (ТЗ §4.1). Беседа = событие, поэтому все выборки
 * фильтруют по {@code event_id} и используют индекс
 * {@code ix_event_message_event_created}.
 *
 * <p>{@code @EntityGraph(attributePaths = "sender")} на «листовых» методах
 * eager-фетчит автора одним join'ом — иначе маппинг каждого сообщения в DTO
 * (имя/аватар отправителя) порождал бы N+1. Для системных сообщений
 * ({@code sender == null}) граф безвреден.
 */
@Repository
public interface EventMessageRepository extends JpaRepository<EventMessage, Long> {

    /** Лента беседы, новые — через {@code Pageable} с {@code Sort by id desc}. */
    @EntityGraph(attributePaths = "sender")
    Page<EventMessage> findByEvent_Id(Long eventId, Pageable pageable);

    /** Скроллбэк: сообщения старше курсора {@code before} (id &lt; before). */
    @EntityGraph(attributePaths = "sender")
    Page<EventMessage> findByEvent_IdAndIdLessThan(Long eventId, Long before, Pageable pageable);

    /** Опрос новых: сообщения новее курсора {@code after} (id &gt; after), по возрастанию. */
    @EntityGraph(attributePaths = "sender")
    List<EventMessage> findByEvent_IdAndIdGreaterThanOrderByIdAsc(Long eventId, Long after, Pageable pageable);

    /** Последнее сообщение беседы — для превью в списке бесед. */
    Optional<EventMessage> findFirstByEvent_IdOrderByIdDesc(Long eventId);

    /** Сколько сообщений новее прочитанного маркера — счётчик непрочитанных. */
    long countByEvent_IdAndIdGreaterThan(Long eventId, Long lastReadMessageId);
}
