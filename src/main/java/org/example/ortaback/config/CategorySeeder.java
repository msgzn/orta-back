package org.example.ortaback.config;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сидер базовых категорий, запускается на старте Spring-контекста.
 *
 * <p>Идемпотентен: на каждой категории сначала {@code findByName} —
 * если уже есть, пропускаем; если нет, вставляем. Это значит:
 * <ul>
 *   <li>первый запуск создаёт всё дерево;</li>
 *   <li>обычный перезапуск ничего не делает;</li>
 *   <li>если кто-то вручную удалил «Футбол» — следующий старт его восстановит.</li>
 * </ul>
 *
 * <p>Отключить можно проперти {@code application.seed.categories.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "application.seed.categories.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CategorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategorySeeder.class);

    private final CategoryRepository categoryRepository;

    /**
     * Дерево категорий: корневая → её дети (могут быть пустые).
     * Имена ровно те, что прислал бизнес — менять без согласования
     * с фронтом не стоит. У корневых задан стабильный {@code slug}
     * (ТЗ §1.4): по нему фронт мапит цвет/emoji, не завязываясь на
     * изменяемое {@code name}; у подкатегорий slug = {@code null}.
     */
    private static final List<RootCategory> TREE = List.of(
            new RootCategory("Спорт", "sport", List.of(
                    "Футбол", "Баскетбол", "Теннис", "Бег",
                    "Тренажёрный зал", "Плавание"
            )),
            new RootCategory("Интеллектуальные активности", "mind", List.of(
                    "Настольные игры", "Шахматы", "Квизы"
            )),
            new RootCategory("Творческие активности", "creative", List.of(
                    "Рисование", "Музицирование", "Фотопрогулки"
            )),
            new RootCategory("Образовательные встречи", "edu", List.of(
                    "Учебные группы", "Языковые клубы", "Хакатоны"
            )),
            new RootCategory("Досуг", "leisure", List.of(
                    "Кино", "Концерты", "Выставки", "Прогулки"
            )),
            new RootCategory("Путешествия и походы", "travel", List.of()),
            new RootCategory("Кулинария и совместные ужины", "food", List.of()),
            new RootCategory("Волонтёрство", "volunteer", List.of()),
            new RootCategory("Онлайн-игры", "games", List.of(
                    "CS2", "FastCup", "PUBG", "Компьютерный клуб"
            ))
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long before = categoryRepository.count();
        for (RootCategory root : TREE) {
            Category rootEntity = ensure(root.name(), null, root.slug());
            for (String child : root.children()) {
                ensure(child, rootEntity, null);
            }
        }
        long after = categoryRepository.count();
        long created = after - before;
        if (created > 0) {
            log.info("CategorySeeder: ensured {} categories ({} new)", after, created);
        } else {
            log.info("CategorySeeder: all {} base categories already present", after);
        }
    }

    /**
     * Возвращает существующую категорию или создаёт новую с указанным
     * родителем и slug. Если категория уже есть, но без slug (засижена до
     * появления поля) — бэкфилл: проставляем slug и сохраняем. Идемпотентно
     * и самовосстанавливаемо, как и остальной сидер.
     */
    private Category ensure(String name, Category parent, String slug) {
        Category existing = categoryRepository.findByName(name).orElse(null);
        if (existing != null) {
            if (slug != null && !slug.equals(existing.getSlug())) {
                existing.setSlug(slug);
                categoryRepository.save(existing);
            }
            return existing;
        }
        return categoryRepository.save(Category.builder()
                .name(name)
                .parent(parent)
                .slug(slug)
                .build());
    }

    private record RootCategory(String name, String slug, List<String> children) {}
}
