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
import org.example.ortaback.persistence.models.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * In-app уведомление (ТЗ §4.2). Хранит факт события, адресованного конкретному
 * пользователю ({@code recipient}); прочитанность определяется по
 * {@code readAt != null} (вместо булева флага — чтобы знать «когда прочитал»).
 *
 * <p>{@code actor}/{@code event} опциональны: например, {@code TRUST_LEVEL_UP}
 * не имеет ни инициатора, ни события. Связи {@code LAZY} — наполняются
 * fetch-join'ом в репозитории при выдаче ленты, чтобы не ловить N+1.
 *
 * <p>Эта сущность отвечает только за <em>in-app</em> ленту. Доставка
 * push/FCM (и её гейтинг через {@code User.notify*}-флаги) — отдельный канал,
 * вне зоны ответственности данной модели.
 */
@Entity
@Table(
        name = "notification",
        indexes = @Index(name = "ix_notification_recipient_created",
                columnList = "recipient_id, created_at")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Кому адресовано уведомление. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** Инициатор (например, присоединившийся пользователь). Может быть {@code null}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Связанное событие. Может быть {@code null} (напр. для {@code TRUST_LEVEL_UP}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Предрассчитанный человекочитаемый текст уведомления. Локаль фиксируется в
     * момент генерации (по {@code recipient.language}); хранить готовую строку
     * проще, чем собирать её в рантайме из шаблонов на каждый запрос ленты.
     */
    @Column(columnDefinition = "text")
    private String text;

    /** Время прочтения; {@code null} — ещё не прочитано. */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
