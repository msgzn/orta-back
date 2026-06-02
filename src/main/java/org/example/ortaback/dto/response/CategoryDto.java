package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Плоский DTO категории. Используется в списках, фильтрах и интересах
 * пользователя. {@code parentId} позволяет фронту локально восстановить
 * иерархию, если он того хочет; готовое дерево отдаётся отдельным
 * эндпоинтом {@code GET /categories/tree}.
 *
 * <p>{@code slug} — стабильный ключ корневой категории для клиентского
 * маппинга цвет/emoji (ТЗ §1.4). У подкатегорий обычно {@code null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryDto(
        Long id,
        Long parentId,
        String slug,
        String name,
        String iconUrl
) {}
