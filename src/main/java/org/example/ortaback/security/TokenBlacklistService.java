package org.example.ortaback.security;

import java.time.Instant;

/**
 * Blacklist отозванных JTI (JWT-ID). Используется для refresh-токенов:
 * <ul>
 *   <li>при ротации в {@code /auth/refresh} — старый JTI попадает в blacklist,
 *       чтобы тот же refresh не сработал второй раз;</li>
 *   <li>при logout — JTI текущего refresh попадает в blacklist до его
 *       естественного истечения.</li>
 * </ul>
 */
public interface TokenBlacklistService {

    /**
     * Помечает JTI как отозванный. TTL равен «осталось до истечения токена»
     * — после этого момента запись из хранилища может уйти без потери
     * безопасности (токен и так истёк по подписи).
     */
    void blacklist(String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);
}
