package org.example.ortaback.dto.response;

/**
 * Ответ Auth-эндпоинтов: пара access + refresh.
 *
 * <p>{@code expiresInMs} — TTL access-токена (миллисекунды); фронт может
 * запланировать обновление до истечения.
 */
public record JwtResponseDto(

        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs
) {

    public static JwtResponseDto bearer(String accessToken, String refreshToken, long expiresInMs) {
        return new JwtResponseDto(accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
