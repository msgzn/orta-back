package org.example.ortaback.dto.response;

/**
 * Результат загрузки аватара: финальный публичный URL для {@code User.avatarUrl}.
 */
public record AvatarUploadResponseDto(
        String avatarUrl
) {}
