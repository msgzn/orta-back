package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Полный профиль пользователя — отдаётся на {@code GET /api/v1/users/me}.
 *
 * <p>{@code interests} — список выбранных пользователем категорий
 * (плоские DTO; иерархию фронт восстановит из {@code parentId}).
 * Email возвращается, чтобы фронт мог отобразить его в шапке профиля,
 * но менять его через PATCH нельзя — это идентификатор для логина.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileDto(
        Long id,
        String firstName,
        String lastName,
        String nickname,
        String email,
        String phoneNumber,
        String avatarUrl,
        UserRole role,
        Float trustScore,
        Integer searchRadiusKm,
        String language,
        UniversityMiniDto university,
        LocalDateTime createdAt,
        List<CategoryDto> interests,
        NotificationPreferencesDto notifications
) {}
