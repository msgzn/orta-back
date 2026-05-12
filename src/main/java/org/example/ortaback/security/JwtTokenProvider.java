package org.example.ortaback.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.ortaback.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Генерация и валидация JWT-токенов (jjwt 0.12.x API).
 *
 * <p>Поддерживаются два «типа» токенов:
 * <ul>
 *   <li>{@link #TYPE_ACCESS} — короткоживущий, передаётся в каждом запросе
 *       в заголовке {@code Authorization: Bearer …};</li>
 *   <li>{@link #TYPE_REFRESH} — длинноживущий, используется только для
 *       получения нового access-токена через {@code /api/v1/auth/refresh}.</li>
 * </ul>
 *
 * <p>Тип записывается в claim {@code "type"}, чтобы фильтр на запросе
 * ({@link JwtAuthenticationFilter}) мог отбраковать refresh-токен,
 * присланный туда, где должен быть access.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE  = "type";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        // 64-символьный hex-секрет (по умолчанию) = 64 байта UTF-8 = 512 бит,
        // под HS256 с запасом (мин. 256 бит).
        this.signingKey = Keys.hmacShaKeyFor(
                properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserDetails user) {
        return buildToken(user, properties.accessTokenExpiration(), TYPE_ACCESS);
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(user, properties.refreshTokenExpiration(), TYPE_REFRESH);
    }

    /** Достаёт subject (= email пользователя). */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get(CLAIM_ROLES);
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * Проверяет подпись и срок действия. Любые ошибки парсинга превращаются
     * в {@code false} — само исключение логируется на debug.
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ internals

    private String buildToken(UserDetails user, long ttlMs, String type) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMs);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of(
                        CLAIM_TYPE,  type,
                        CLAIM_ROLES, roles
                ))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
