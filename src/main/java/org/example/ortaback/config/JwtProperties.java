package org.example.ortaback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Биндинг секции {@code application.security.jwt.*} из {@code application.yml}.
 *
 * <p>Сроки жизни токенов задаются в миллисекундах, чтобы напрямую подставлять
 * в {@code Date.from(now.plusMillis(...))}.
 */
@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtProperties(
        String secretKey,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String issuer
) {}
