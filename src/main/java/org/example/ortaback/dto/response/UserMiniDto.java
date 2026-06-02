package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Компактное представление пользователя для списков соц-графа (ТЗ §4.4):
 * подписчики / подписки. {@code NON_NULL} — необязательные поля
 * ({@code nickname}, {@code avatarUrl}) опускаются, если пусты.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserMiniDto(
        Long id,
        String firstName,
        String lastName,
        String nickname,
        String avatarUrl,
        Float trustScore
) {
}
