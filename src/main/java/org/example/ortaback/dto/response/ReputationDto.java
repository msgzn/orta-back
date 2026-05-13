package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Ответ {@code GET /api/v1/users/me/reputation}:
 * <ul>
 *   <li>{@code currentScore} — текущее значение {@code User.trustScore};</li>
 *   <li>{@code last30DaysDelta} — сумма изменений за последние 30 дней
 *       (для прогресс-бара «Reputation: last 30 days»);</li>
 *   <li>{@code history} — последние N записей истории (новые сверху).</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReputationDto(
        Float currentScore,
        Integer last30DaysDelta,
        List<TrustScoreEventDto> history
) {}
