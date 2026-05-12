package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Плоский DTO категории. Используется в списках, фильтрах и интересах
 * пользователя. {@code parentId} позволяет фронту локально восстановить
 * иерархию, если он того хочет; готовое дерево отдаётся отдельным
 * эндпоинтом {@code GET /categories/tree}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryDto(
        Long id,
        Long parentId,
        String name,
        String iconUrl
) {}
