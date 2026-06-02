package org.example.ortaback.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Тело запроса «отметить прочитанным до сообщения {@code lastMessageId}
 * включительно» (ТЗ §4.1). Маркер монотонный — назад не откатывается.
 */
public record MarkReadRequestDto(
        @NotNull
        Long lastMessageId
) {
}
