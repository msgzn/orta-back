package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * PATCH профиля. {@code null} = «не менять».
 *
 * <p>Email и аватар через этот DTO не меняются:
 * <ul>
 *   <li>email — идентификатор для логина, смена потребует отдельной процедуры
 *       с подтверждением;</li>
 *   <li>аватар — управляется парой {@code POST/DELETE /me/avatar}.</li>
 * </ul>
 *
 * <p>Уведомления имеют отдельный эндпоинт {@code PATCH /me/notifications}
 * (тоже опциональны здесь, чтобы можно было обновить всё одной мутацией —
 * Flutter любит мерджить экраны).
 */
public record UserProfileUpdateRequestDto(

        @Size(min = 1, max = 100) String firstName,
        @Size(min = 1, max = 100) String lastName,

        /** ASCII-нiknames: буквы/цифры/подчёркивание/дефис, 3..50 символов. */
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,50}$",
                 message = "Nickname must be 3-50 chars of letters, digits, underscore or hyphen")
        String nickname,

        @Size(max = 20) String phoneNumber,

        @Min(1) @Max(100) Integer searchRadiusKm,

        @Pattern(regexp = "^(ru|en|kk)$", message = "Language must be one of: ru, en, kk")
        String language,

        Boolean notifyEventStart,
        Boolean notifyCapacityReached,
        Boolean notifyEventPublished,
        Boolean notifyReviewReceived
) {}
