package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Тело {@code POST /events/{id}/reviews/venue}.
 * Любая сторона оценивает площадку 1–5 ⭐.
 */
public record VenueRatingRequestDto(

        @NotNull @Min(1) @Max(5) Integer stars
) {}
