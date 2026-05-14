package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Тело {@code POST /auth/logout}. Refresh-токен помечается отозванным
 * (JTI попадает в blacklist до своего естественного истечения).
 */
public record LogoutRequestDto(

        @NotBlank String refreshToken
) {}
