package org.example.ortaback.dto.geo;

import jakarta.validation.constraints.NotNull;
import org.example.ortaback.geo.annotation.ValidLatitude;
import org.example.ortaback.geo.annotation.ValidLongitude;

/**
 * Тело {@code POST /api/v1/users/me/location}: мобильный клиент периодически
 * шлёт сюда свежие координаты пользователя.
 */
public record LocationUpdateRequestDto(

        @NotNull
        @ValidLatitude
        Double lat,

        @NotNull
        @ValidLongitude
        Double lng
) {}
