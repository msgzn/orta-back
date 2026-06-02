package org.example.ortaback.persistence.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Категория события (спорт, бег, баскетбол, …).
 *
 * <p>Поддерживает иерархию через self-referencing {@code parent_id} — это
 * позволяет описывать корневые категории («Спорт») и подкатегории
 * («Баскетбол»). Иконка ({@code icon_url}) используется фронтом для рендера
 * разных типов маркеров на карте.
 */
@Entity
@Table(name = "category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false, length = 100)
    private String name;

    /** URL иконки для отображения категории на карте. Может быть {@code null}. */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Стабильный машинный ключ КОРНЕВОЙ категории ({@code sport/mind/games/…}).
     * Фронт использует его как ключ к presentation-маппингу цвет/emoji (ТЗ §1.4):
     * {@code name} может меняться, {@code slug} — нет. У подкатегорий обычно
     * {@code null} (цвет наследуется от корня). Заполняется сидером, не
     * пользователем; уникальность гарантирует сидер (без DB-constraint, чтобы
     * не цеплять {@code ddl-auto:update}).
     */
    @Column(name = "slug", length = 50)
    private String slug;

    // --- Обратные связи -----------------------------------------------------

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Category> children = new HashSet<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Event> events = new HashSet<>();
}
