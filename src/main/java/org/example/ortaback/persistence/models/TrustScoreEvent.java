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
import org.example.ortaback.persistence.models.enums.TrustReason;

import java.time.LocalDateTime;

/**
 * История изменений {@code User.trustScore}. Каждое изменение балла
 * (плюс/минус) пишется отдельной записью с {@link TrustReason} и
 * (опц.) ссылкой на событие, из которого пришло изменение.
 *
 * <p>Используется для:
 * <ul>
 *   <li>агрегата «Last 30 days» в профиле;</li>
 *   <li>списка «Rating history: Football match +5» в Reputation-разделе.</li>
 * </ul>
 */
@Entity
@Table(
        name = "trust_score_event",
        indexes = {
                @Index(name = "idx_tse_user",       columnList = "user_id"),
                @Index(name = "idx_tse_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TrustScoreEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Сумма к trust_score: положительная или отрицательная. */
    @Column(nullable = false)
    private Integer delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrustReason reason;

    /** Событие-источник изменения (если применимо). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
