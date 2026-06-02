package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Узел иерархического дерева категорий: те же поля, что в {@link CategoryDto}
 * (включая {@code slug} корневой категории), плюс вложенные дети. Используется
 * для пикера на экране создания события.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryTreeNodeDto(
        Long id,
        Long parentId,
        String slug,
        String name,
        String iconUrl,
        List<CategoryTreeNodeDto> children
) {}
