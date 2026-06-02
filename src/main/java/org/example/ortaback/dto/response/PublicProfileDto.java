package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * «Check the owner» — публичный профиль произвольного пользователя.
 * Никогда не отдаёт почту, телефон, координаты, эмбеддинги и настройки.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicProfileDto(
        Long id,
        String firstName,
        String lastName,
        String nickname,
        String avatarUrl,
        Float trustScore,
        UniversityMiniDto university,
        LocalDateTime createdAt,
        List<TrustScoreEventDto> recentActivity,
        UserStatsDto stats
) {}
