package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Публичный DTO университета.
 *
 * <p>{@code domainPattern} — основной домен для отображения («ты
 * регистрируешься как студент X»). {@code domains} — полный набор доменов
 * ВУЗа, по которым идёт авто-привязка по email (суффикс-матч на бэке).
 *
 * <p>Координаты кампуса намеренно не выставляем — для публичного reference
 * не нужны; если потребуются для карты — добавим отдельный эндпоинт.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversityDto(
        Long id,
        String name,
        String domainPattern,
        List<String> domains,
        String address
) {}
