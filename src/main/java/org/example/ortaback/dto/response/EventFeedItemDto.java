package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Карточка события в общей ленте {@code GET /api/v1/events}.
 *
 * <p>Облегчённая версия {@link org.example.ortaback.dto.geo.EventDetailDto} —
 * без {@code description}, без эмбеддингов, но с {@link CreatorMiniDto}
 * (фронт показывает имя/аватар/trust score организатора прямо в ленте).
 *
 * <p>Координаты учитывают приватность: для APPROXIMATE-событий не присоединившимся
 * вьюерам возвращается снэппнутая точка через {@code PrivacyMasker}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventFeedItemDto(
        Long id,
        String title,
        String placeName,
        String address,

        Double lat,
        Double lng,
        LocationPrecision locationPrecision,

        LocalDateTime startTime,
        Integer timeDuration,
        Integer maxParticipants,
        long participantsCount,
        BigDecimal pricePerPerson,
        EventStatus status,

        Long categoryId,
        String categoryName,
        String categoryIconUrl,

        CreatorMiniDto creator
) {}
