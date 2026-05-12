package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * PATCH профиля. {@code null} = «не менять».
 *
 * <p>Email и аватар через этот DTO не меняются:
 * <ul>
 *   <li>email — идентификатор для логина, смена потребует отдельной процедуры
 *       с подтверждением;</li>
 *   <li>аватар — управляется парой {@code POST/DELETE /me/avatar}, потому что
 *       это бинарный ресурс с пред/постусловиями в S3.</li>
 * </ul>
 */
public record UserProfileUpdateRequestDto(

        @Size(min = 1, max = 100) String firstName,
        @Size(min = 1, max = 100) String lastName,
        @Size(max = 20) String phoneNumber,

        @Min(1) @Max(100) Integer searchRadiusKm
) {}
