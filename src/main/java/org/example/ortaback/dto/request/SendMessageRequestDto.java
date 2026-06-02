package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса на отправку сообщения в чат события (ТЗ §4.1).
 */
public record SendMessageRequestDto(
        @NotBlank
        @Size(max = 2000)
        String text
) {
}
