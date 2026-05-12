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
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.type.VectorType;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Point;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Пользователь платформы.
 *
 * <p>Реализует {@link UserDetails}, чтобы напрямую использоваться Spring Security.
 * В качестве логина выступает {@code email} (он уникален; поле
 * {@link UserDetails#getUsername()} возвращает именно его).
 *
 * <p>Геолокация ({@code last_known_location}) — последняя известная позиция
 * пользователя, нужна для фильтра «события рядом». Согласно ТЗ, никогда не
 * отдаётся в публичных API, поэтому на уровне DTO/сериализации поле
 * не маппится наружу.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /** BCrypt-хеш пароля. Никогда не отдаётся клиенту. */
    @JsonIgnore
    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "trust_score", nullable = false, columnDefinition = "real default 0.0")
    @Builder.Default
    private Float trustScore = 0.0f;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Эмбеддинг глобальных интересов пользователя (агрегат по
     * {@link UserInterest}) — используется для AI-матчинга через pgvector
     * (оператор cosine-distance {@code <=>}).
     */
    @JsonIgnore
    @Type(VectorType.class)
    @Column(name = "global_interest_embedding", columnDefinition = "vector(384)")
    private float[] globalInterestEmbedding;

    /**
     * Последняя известная геопозиция пользователя (SRID 4326).
     * Обновляется мобильным клиентом периодически; в публичных API не
     * сериализуется (приватность).
     */
    @JsonIgnore
    @Column(name = "last_known_location", columnDefinition = "geometry(Point,4326)")
    private Point lastKnownLocation;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    /**
     * Радиус поиска событий по умолчанию (км). Если {@code null} — используется
     * значение из {@code application.geo.default-search-radius-km}.
     */
    @Column(name = "search_radius_km", nullable = false, columnDefinition = "integer default 10")
    @Builder.Default
    private Integer searchRadiusKm = 10;

    // --- Обратные связи -----------------------------------------------------

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserInterest> interests = new HashSet<>();

    @OneToMany(mappedBy = "creator", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Event> createdEvents = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<EventParticipant> participations = new HashSet<>();

    // --- Жизненный цикл -----------------------------------------------------

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.trustScore == null) {
            this.trustScore = 0.0f;
        }
        if (this.searchRadiusKm == null) {
            this.searchRadiusKm = 10;
        }
    }

    // --- UserDetails --------------------------------------------------------

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Для Spring Security используем email как «username». */
    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return hashedPassword;
    }

    @Override @JsonIgnore public boolean isAccountNonExpired()     { return true; }
    @Override @JsonIgnore public boolean isAccountNonLocked()      { return true; }
    @Override @JsonIgnore public boolean isCredentialsNonExpired() { return true; }
    @Override @JsonIgnore public boolean isEnabled()               { return true; }
}
