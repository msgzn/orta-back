package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Публичный DTO университета. Содержит {@code domainPattern} — фронт
 * использует это поле для подсветки «ты регистрируешься как студент X
 * Университета» во время ввода email на экране регистрации.
 *
 * <p>Координаты кампуса намеренно не выставляем — для публичного reference
 * не нужны; если потребуются для карты — добавим отдельный эндпоинт.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversityDto(
        Long id,
        String name,
        String domainPattern,
        String address
) {}
