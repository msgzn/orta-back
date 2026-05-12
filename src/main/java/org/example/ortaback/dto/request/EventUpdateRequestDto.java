package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.example.ortaback.geo.annotation.ValidLatitude;
import org.example.ortaback.geo.annotation.ValidLongitude;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PATCH-семантика: все поля опциональны. {@code null} означает «не менять».
 *
 * <p>Поля {@code lat}/{@code lng} должны передаваться вместе — это
 * проверяется сервисным слоем (Bean Validation не выражает «оба или ни одного»
 * без кастомного валидатора).
 */
public record EventUpdateRequestDto(

        @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Size(max = 200) String placeName,
        @Size(max = 500) String address,

        @ValidLatitude  Double lat,
        @ValidLongitude Double lng,
        LocationPrecision locationPrecision,

        Long categoryId,

        @Future LocalDateTime startTime,
        @Positive Integer timeDuration,

        @PositiveOrZero Integer minParticipants,
        @PositiveOrZero Integer maxParticipants,

        @PositiveOrZero BigDecimal pricePerPerson,

        EventStatus status
) {}
