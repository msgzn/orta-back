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
     * с фронтом не стоит, иконки/локализацию фронт мапит по name.
     */
    private static final List<RootCategory> TREE = List.of(
            new RootCategory("Спорт", List.of(
                    "Футбол", "Баскетбол", "Теннис", "Бег",
                    "Тренажёрный зал", "Плавание"
            )),
            new RootCategory("Интеллектуальные активности", List.of(
                    "Настольные игры", "Шахматы", "Квизы"
            )),
            new RootCategory("Творческие активности", List.of(
                    "Рисование", "Музицирование", "Фотопрогулки"
            )),
            new RootCategory("Образовательные встречи", List.of(
                    "Учебные группы", "Языковые клубы", "Хакатоны"
            )),
            new RootCategory("Досуг", List.of(
                    "Кино", "Концерты", "Выставки", "Прогулки"
            )),
            new RootCategory("Путешествия и походы", List.of()),
            new RootCategory("Кулинария и совместные ужины", List.of()),
            new RootCategory("Волонтёрство", List.of()),
            new RootCategory("Онлайн-игры", List.of(
                    "CS2", "FastCup", "PUBG", "Компьютерный клуб"
            ))
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long before = categoryRepository.count();
        for (RootCategory root : TREE) {
            Category rootEntity = ensure(root.name(), null);
            for (String child : root.children()) {
                ensure(child, rootEntity);
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
     * родителем.
     */
    private Category ensure(String name, Category parent) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .parent(parent)
                        .build()));
    }

    private record RootCategory(String name, List<String> children) {}
}
