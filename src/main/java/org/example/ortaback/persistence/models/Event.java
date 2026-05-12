package org.example.ortaback.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.type.VectorType;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Спортивное событие.
 *
 * <p>Ключевая сущность платформы для карты: поле {@link #location} —
 * {@code geometry(Point,4326)} — используется в пространственных запросах через
 * PostGIS ({@code ST_DWithin}, {@code ST_MakeEnvelope}, оператор {@code <->}).
 *
 * <p>Поле {@link #eventEmbedding} (pgvector) — для AI-матчинга «похожих
 * событий» через cosine-distance {@code <=>}.
 *
 * <p>NB: GiST-индекс на {@code location} объявляем не через {@code @Index}
 * (Hibernate сгенерировал бы B-tree, который на geometry не работает),
 * а отдельной миграцией Flyway / руками в {@code init.sql}:
 * {@code CREATE INDEX idx_event_location ON event USING GIST (location);}.
 * B-tree-индексы, объявленные ниже на скалярные поля, безопасны.
 */
@Entity
@Table(
        name = "event",
        indexes = {
                @Index(name = "idx_event_status",      columnList = "status"),
                @Index(name = "idx_event_start_time",  columnList = "start_time"),
                @Index(name = "idx_event_category_id", columnList = "category_id"),
                @Index(name = "idx_event_creator_id",  columnList = "creator_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.ACTIVE;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Название места (например, «Спортзал КазНУ»). */
    @Column(name = "place_name", length = 200)
    private String placeName;

    /** Полный текстовый адрес для карточки события. */
    @Column(length = 500)
    private String address;

    /**
     * Географические координаты события (SRID 4326 — WGS 84).
     * Критическое поле для отображения на карте и пространственного поиска.
     */
    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    /**
     * Точная локация или приблизительная зона. Для {@link LocationPrecision#APPROXIMATE}
     * сервер не отдаёт реальные координаты не присоединившимся пользователям,
     * а возвращает «снэп» через {@code ST_SnapToGrid}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "location_precision", nullable = false, length = 20)
    @Builder.Default
    private LocationPrecision locationPrecision = LocationPrecision.EXACT;

    @Column(name = "min_participants")
    private Integer minParticipants;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** Длительность события в минутах. */
    @Column(name = "time_duration")
    private Integer timeDuration;

    @Column(name = "price_per_person", precision = 10, scale = 2)
    private BigDecimal pricePerPerson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Эмбеддинг события (тематика + описание + категория) для AI-матчинга
     * через pgvector ({@code <=> } cosine-distance).
     */
    @JsonIgnore
    @Type(VectorType.class)
    @Column(name = "event_embedding", columnDefinition = "vector(384)")
    private float[] eventEmbedding;

    // --- Обратные связи -----------------------------------------------------

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EventParticipant> participants = new HashSet<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = EventStatus.ACTIVE;
        }
        if (this.locationPrecision == null) {
            this.locationPrecision = LocationPrecision.EXACT;
        }
    }
}
