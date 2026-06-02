package org.example.ortaback.dto.geo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Полная карточка события (запрашивается при клике на маркер).
 *
 * <p>Координаты также учитывают приватность: если viewer не присоединился к
 * APPROXIMATE-событию и не является его создателем, lat/lng возвращаются
 * маскированными.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventDetailDto(
        Long id,
        String title,
        String description,
        String placeName,
        String address,
        Double lat,
        Double lng,
        LocalDateTime startTime,
        Integer timeDuration,
        Integer minParticipants,
        Integer maxParticipants,
        BigDecimal pricePerPerson,
        EventStatus status,
        LocationPrecision locationPrecision,

        Long categoryId,
        String categoryName,
        String categoryIconUrl,

        Long creatorId,
        String creatorFirstName,
        String creatorLastName,
        String creatorAvatarUrl,

        /**
         * Trust score организатора (0..100). Нужен на карточке события,
         * чтобы фронт не делал второй запрос к {@code /users/{id}/public-profile}
         * ради одной цифры.
         */
        Float creatorTrustScore,

        long participantsCount,

        /**
         * Сколько пользователей «наблюдают» за событием (Observe) — реальный
         * COUNT по {@code event_observation}. В отличие от участников,
         * наблюдатели не входят в capacity и не видны в списке участников.
         */
        long observersCount,

        /** Hero / обложка события из S3. {@code null} → фронт показывает плейсхолдер. */
        String coverImageUrl
) {}
