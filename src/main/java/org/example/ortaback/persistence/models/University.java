package org.example.ortaback.persistence.models;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Университет / учебное заведение.
 *
 * <p>Используется для верификации студентов по {@code domain_pattern} email
 * и для группировки событий на карте вокруг главного кампуса
 * ({@code campus_location}).
 */
@Entity
@Table(name = "university")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    /**
     * Основной домен для ОТОБРАЖЕНИЯ («ты регистрируешься как студент X»),
     * напр. {@code kaznu.kz}. Авто-привязка по почте матчится НЕ по этому
     * одному полю, а по полному набору {@link #domains} (суффикс-матч);
     * primary-домен входит в {@code domains} тоже.
     */
    @Column(name = "domain_pattern", nullable = false, length = 100)
    private String domainPattern;

    /**
     * Все домены ВУЗа для авто-детекта по email — голые, без {@code @}
     * (напр. {@code kaznu.kz}, {@code farabi.university}). Матчинг суффиксный:
     * host письма равен домену ИЛИ заканчивается на {@code .<домен>}
     * (так {@code @stu.kaznu.kz} попадает в {@code kaznu.kz}).
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "university_domain",
            joinColumns = @JoinColumn(name = "university_id"))
    @Column(name = "domain", nullable = false, length = 100)
    @Builder.Default
    private Set<String> domains = new HashSet<>();

    /**
     * Координаты главного кампуса (SRID 4326 — WGS 84).
     * GiST-индекс создаётся миграцией: {@code CREATE INDEX ... USING GIST}.
     */
    @Column(name = "campus_location", columnDefinition = "geometry(Point,4326)")
    private Point campusLocation;

    /** Полный текстовый адрес кампуса. */
    @Column(length = 500)
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- Обратные связи -----------------------------------------------------

    @OneToMany(mappedBy = "university", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
