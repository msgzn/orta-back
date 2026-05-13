package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * «Наблюдатель» события — пользователь подписался следить за событием, но
 * пока не присоединился. Используется для:
 * <ul>
 *   <li>таба «Observed» в профиле;</li>
 *   <li>push-нотификации «There are enough members», когда событие набирает
 *       {@code minParticipants}.</li>
 * </ul>
 *
 * <p>Сущность вынесена отдельно от {@code EventParticipant} (а не сделана
 * новым {@code ParticipantStatus}), потому что семантика принципиально иная:
 * observer не учитывается в capacity, не показывается в списке участников,
 * не получает «start event» push.
 */
@Entity
@Table(
        name = "event_observation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_observation",
                columnNames = {"event_id", "user_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EventObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
