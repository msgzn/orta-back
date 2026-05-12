package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.example.ortaback.geo.annotation.ValidLatitude;
import org.example.ortaback.geo.annotation.ValidLongitude;
import org.example.ortaback.persistence.models.enums.LocationPrecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventCreateRequestDto(

        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Size(max = 200) String placeName,
        @Size(max = 500) String address,

        @NotNull @ValidLatitude  Double lat,
        @NotNull @ValidLongitude Double lng,
        LocationPrecision locationPrecision,

        @NotNull Long categoryId,

        @NotNull @Future LocalDateTime startTime,
        @Positive Integer timeDuration,

        @PositiveOrZero Integer minParticipants,
        @PositiveOrZero Integer maxParticipants,

        @PositiveOrZero BigDecimal pricePerPerson
) {}
