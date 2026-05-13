package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Тело {@code POST /events/{id}/reviews/promoter}.
 * Участник оценивает промоутера события.
 */
public record PromoterReviewRequestDto(

        @NotNull Boolean came,
        @Size(max = 2000) String comment
) {}
