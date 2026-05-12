package org.example.ortaback.dto.geo;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.ortaback.persistence.models.enums.LocationPrecision;

/**
 * Облегчённый DTO для отображения маркера на карте.
 *
 * <p>Отдаётся пачкой при загрузке видимой области; фронт рендерит маркеры
 * без подгрузки полной информации о событиях. Полный набор данных
 * запрашивается через {@code EventDetailDto} при клике на маркер.
 *
 * <p>Координаты {@code lat}/{@code lng} уже учитывают приватность: для
 * APPROXIMATE-событий «чужие» пользователи получают точку, снэппнутую к
 * сетке через {@code PrivacyMasker}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventMapMarkerDto(
        Long id,
        String title,
        Double lat,
        Double lng,
        String categoryIcon,
        long participantsCount,
        LocationPrecision locationPrecision
) {}
