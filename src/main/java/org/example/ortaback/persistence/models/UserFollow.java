package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Подписка одного пользователя на другого (ТЗ §4.4 «Друзья», минимальная
 * follow-модель v2). Однонаправленное ребро соц-графа: {@code follower}
 * подписан на {@code followee}.
 *
 * <p>Уникальность {@code (follower_id, followee_id)} запрещает дубли подписки;
 * индекс по {@code followee_id} ускоряет выборку списка подписчиков
 * («кто на меня подписан»).
 */
@Entity
@Table(
        name = "user_follows",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_follow_follower_followee",
                columnNames = {"follower_id", "followee_id"}),
        indexes = @Index(name = "ix_user_follow_followee", columnList = "followee_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Тот, кто подписывается. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    /** Тот, на кого подписываются. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
