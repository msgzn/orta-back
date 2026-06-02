package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.ortaback.persistence.models.enums.MessageType;

import java.time.LocalDateTime;

/**
 * Сообщение чата события (ТЗ §4.1). Беседа-контейнер — само {@link Event};
 * участники беседы = {@link EventParticipant} со статусом {@code JOINED}
 * (+ creator). Системные сообщения ({@link MessageType#SYSTEM}) имеют
 * {@code sender == null} и генерируются {@code EventService} на
 * join/leave/start/finish/cancel.
 *
 * <p>Индекс {@code (event_id, created_at)} обслуживает основной паттерн
 * выборки — лента сообщений одной беседы по времени.
 */
@Entity
@Table(
        name = "event_message",
        indexes = @Index(name = "ix_event_message_event_created",
                columnList = "event_id, created_at")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EventMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Беседа-контейнер = событие. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** Автор. {@code null} для системных сообщений ({@code type = SYSTEM}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageType type = MessageType.USER;

    @Column(columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.type == null) {
            this.type = MessageType.USER;
        }
    }
}
