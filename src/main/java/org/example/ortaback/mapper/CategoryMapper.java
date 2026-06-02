package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.CategoryTreeNodeDto;
import org.example.ortaback.persistence.models.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Category} → flat-/tree-DTO.
 *
 * <p>Дерево собирается в памяти из плоского списка одной проходкой —
 * избегаем рекурсивных SQL и проблемы Hibernate с lazy children
 * ({@code @OneToMany(mappedBy = "parent")} триггерит lazy при каждой
 * итерации).
 */
@Component
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        return new CategoryDto(
                category.getId(),
                parentId,
                category.getSlug(),
                category.getName(),
                category.getIconUrl()
        );
    }

    /**
     * Собирает дерево из плоского списка категорий. Узлы упорядочиваются
     * стабильно по id; дети — по name.
     */
    public List<CategoryTreeNodeDto> toTree(List<Category> categories) {
        // 1. Превратили в мутабельные узлы с пустым children.
        Map<Long, MutableNode> byId = new HashMap<>();
        for (Category c : categories) {
            byId.put(c.getId(), new MutableNode(
                    c.getId(),
                    c.getParent() != null ? c.getParent().getId() : null,
                    c.getSlug(),
                    c.getName(),
                    c.getIconUrl()
            ));
        }
        // 2. Прицепили детей к родителям; собрали roots.
        List<MutableNode> roots = new ArrayList<>();
        for (MutableNode node : byId.values()) {
            if (node.parentId == null || !byId.containsKey(node.parentId)) {
                roots.add(node);
            } else {
                byId.get(node.parentId).children.add(node);
            }
        }
        // 3. Сортируем стабильно.
        roots.sort(Comparator.comparing(n -> n.name));
        for (MutableNode n : byId.values()) {
            n.children.sort(Comparator.comparing(c -> c.name));
        }
        // 4. Перенесли в immutable record-дерево.
        return roots.stream().map(this::freeze).toList();
    }

    private CategoryTreeNodeDto freeze(MutableNode node) {
        List<CategoryTreeNodeDto> frozenChildren = node.children.stream()
                .map(this::freeze)
                .toList();
        return new CategoryTreeNodeDto(
                node.id, node.parentId, node.slug, node.name, node.iconUrl,
                frozenChildren.isEmpty() ? null : frozenChildren
        );
    }

    private static final class MutableNode {
        final Long id;
        final Long parentId;
        final String slug;
        final String name;
        final String iconUrl;
        final List<MutableNode> children = new ArrayList<>();

        MutableNode(Long id, Long parentId, String slug, String name, String iconUrl) {
            this.id = id;
            this.parentId = parentId;
            this.slug = slug;
            this.name = name;
            this.iconUrl = iconUrl;
        }
    }
}
