package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Маркер прочтения чата события (ТЗ §4.1): для пары (событие, пользователь)
 * хранит id последнего прочитанного сообщения. Непрочитанные =
 * {@code count(event_message WHERE id > last_read_message_id)}.
 *
 * <p>Уникальность {@code (event_id, user_id)} гарантирует одну строку-маркер
 * на участника беседы; обновляется как при явном {@code markRead}, так и при
 * отправке собственного сообщения (своё сообщение не считается непрочитанным).
 */
@Entity
@Table(
        name = "event_message_read",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_event_message_read_event_user",
                columnNames = {"event_id", "user_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EventMessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Id последнего прочитанного {@link EventMessage} в этой беседе. */
    @Column(name = "last_read_message_id", nullable = false)
    private Long lastReadMessageId;
}
