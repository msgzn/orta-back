package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.TrustReason;

import java.time.LocalDateTime;

/**
 * Одна запись истории trust-score'а. На UI рендерится как
 * «Football match · +5 · 2 дня назад».
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrustScoreEventDto(
        Long id,
        Integer delta,
        TrustReason reason,
        Long eventId,
        String eventTitle,
        LocalDateTime createdAt
) {}
