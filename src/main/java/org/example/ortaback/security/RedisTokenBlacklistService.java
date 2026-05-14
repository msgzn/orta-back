package org.example.ortaback.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-реализация {@link TokenBlacklistService}.
 *
 * <p>Ключ: {@code jwt:blacklist:<jti>}; значение — пустышка {@code "1"};
 * TTL — Time-to-Live из самого токена (до его natural expiration).
 *
 * <p>Если Redis недоступен — fail-open: метки не пишутся, проверки
 * возвращают {@code false}. Это даёт удобный dev-режим без поднятого
 * Redis'а; production-инфра должна обеспечить Redis доступным.
 */
@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklistService.class);

    /** Префикс ключей — отделяет blacklist от других данных в Redis. */
    static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            // токен уже истёк — нет смысла занимать слот.
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable, JTI {} not blacklisted: {}", jti, ex.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            Boolean exists = redis.hasKey(KEY_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException ex) {
            // fail-open: при недоступном Redis работаем без revocation.
            log.warn("Redis unavailable, treating JTI {} as not blacklisted: {}", jti, ex.getMessage());
            return false;
        }
    }
}
