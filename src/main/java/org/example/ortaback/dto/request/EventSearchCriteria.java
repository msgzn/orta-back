package org.example.ortaback.dto.request;

import java.time.LocalDateTime;

/**
 * Критерии расширенного поиска событий (ТЗ §4.3). Все поля опциональны и
 * комбинируются по AND; {@code null} означает «фильтр не задан». Собирается
 * контроллером из query-параметров {@code GET /api/v1/events/search}.
 *
 * <p>Гео-фильтр — «всё или ничего»: {@code lat}, {@code lng} и
 * {@code maxDistanceM} передаются вместе либо не передаются вовсе (валидируется
 * в сервисе).
 *
 * <p><b>Не поддержано в v1</b> (намеренно нет полей):
 * <ul>
 *   <li>«с друзьями» — требует соц-графа (ТЗ §4.4);</li>
 *   <li>«с едой» — требует нового поля {@code events.has_refreshments}
 *       (отдельное продуктовое решение + чекбокс на Create).</li>
 * </ul>
 *
 * @param q                 текст для full-text (ILIKE по title + description)
 * @param categoryId        фильтр по категории
 * @param from              нижняя граница {@code startTime} (включительно)
 * @param to                верхняя граница {@code startTime} (включительно)
 * @param lat               широта центра гео-поиска
 * @param lng               долгота центра гео-поиска
 * @param maxDistanceM      радиус в метрах ({@code ST_DWithin})
 * @param free              {@code true} — только бесплатные (цена null или 0);
 *                          {@code false} — только платные
 * @param minGroup          минимальный размер группы ({@code maxParticipants >=})
 * @param maxGroup          максимальный размер группы ({@code maxParticipants <=})
 * @param organizerMinTrust минимальный trust-score организатора (Trusted+ → 30)
 */
public record EventSearchCriteria(
        String q,
        Long categoryId,
        LocalDateTime from,
        LocalDateTime to,
        Double lat,
        Double lng,
        Double maxDistanceM,
        Boolean free,
        Integer minGroup,
        Integer maxGroup,
        Float organizerMinTrust
) {}
