package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Тело {@code POST /events/{id}/reviews/participants}.
 * Промоутер оценивает одного участника.
 */
public record ParticipantReviewRequestDto(

        @NotNull Long userId,
        @NotNull Boolean came,
        @Size(max = 2000) String comment
) {}
