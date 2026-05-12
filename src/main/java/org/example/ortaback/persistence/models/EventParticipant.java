package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;

import java.time.LocalDateTime;

/**
 * Участник события.
 *
 * <p>Связь {@code N:M} между {@link User} и {@link Event}, вынесенная в
 * сущность ради дополнительных полей: статус ({@link ParticipantStatus}),
 * флаг фактического присутствия ({@code attend_at}) и метка присоединения.
 *
 * <p>Уникальная пара {@code (event_id, user_id)} гарантирует, что один и тот
 * же пользователь не присоединится к одному событию дважды.
 */
@Entity
@Table(
        name = "event_participant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_participant",
                columnNames = {"event_id", "user_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.JOINED;

    /**
     * Флаг фактического присутствия на событии (отмечается после {@code DONE}).
     * Имя поля сохранено по ТЗ.
     */
    @Column(name = "attend_at")
    private Boolean attendAt;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        this.joinedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ParticipantStatus.JOINED;
        }
    }
}
