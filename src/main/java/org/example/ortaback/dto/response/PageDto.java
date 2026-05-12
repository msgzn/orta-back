package org.example.ortaback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Облегчённое DTO страницы — отдаётся вместо Spring'овского {@code Page},
 * чтобы не светить во внешнем API внутренние поля
 * ({@code pageable}, {@code sort.unsorted}, {@code numberOfElements}…).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public static <T> PageDto<T> of(Page<T> page) {
        return new PageDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
