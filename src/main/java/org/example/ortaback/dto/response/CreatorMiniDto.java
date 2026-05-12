package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Краткое представление создателя события — встраивается в карточки
 * ленты и в детальный DTO события.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatorMiniDto(
        Long id,
        String firstName,
        String lastName,
        String avatarUrl,
        Float trustScore
) {}
