package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Краткое представление университета внутри других DTO (например, профиля). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UniversityMiniDto(
        Long id,
        String name
) {}
