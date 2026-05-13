package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Настройки пуш-нотификаций — отдаются внутри {@link UserProfileDto} и через
 * {@code PATCH /users/me/notifications}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationPreferencesDto(
        Boolean eventStart,
        Boolean capacityReached,
        Boolean eventPublished,
        Boolean reviewReceived
) {}
