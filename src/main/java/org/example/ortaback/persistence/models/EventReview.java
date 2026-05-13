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
import org.example.ortaback.persistence.models.enums.ReviewType;
import org.example.ortaback.persistence.models.enums.Sentiment;

import java.time.LocalDateTime;

/**
 * Отзыв, оставленный после завершения события.
 *
 * <p>Три семантики ({@link ReviewType}):
 * <ul>
 *   <li>{@code PARTICIPANT_BY_PROMOTER} — {@code reviewer} = промоутер,
 *       {@code target} = оцениваемый участник.</li>
 *   <li>{@code PROMOTER_BY_PARTICIPANT} — {@code reviewer} = участник,
 *       {@code target} = промоутер.</li>
 *   <li>{@code VENUE} — {@code reviewer} = участник или промоутер,
 *       {@code target} = {@code null}, {@code starRating} 1..5.</li>
 * </ul>
 *
 * <p>Уникальность пары «один отзыв на тип/пару/событие» обеспечивается
 * на уровне сервиса (existsBy-проверка перед insert), а не уникальным
 * индексом — последний кошмарит NULL'ы в target_user_id для VENUE-отзывов.
 */
@Entity
@Table(
        name = "event_review",
        indexes = {
                @Index(name = "idx_event_review_event",  columnList = "event_id"),
                @Index(name = "idx_event_review_target", columnList = "target_user_id"),
                @Index(name = "idx_event_review_type",   columnList = "type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class EventReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    /** {@code null} для {@link ReviewType#VENUE}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewType type;

    /** Пришёл ли оцениваемый. {@code null} для VENUE-отзывов. */
    @Column(name = "came")
    private Boolean came;

    /** 1..5 звёзд. Заполнено только для VENUE. */
    @Column(name = "star_rating")
    private Integer starRating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sentiment sentiment;

    /** Сколько баллов trust_score изменилось из-за этого отзыва (+5/-5/-10/-20/0). */
    @Column(name = "trust_delta", nullable = false)
    private Integer trustDelta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.trustDelta == null) {
            this.trustDelta = 0;
        }
    }
}
