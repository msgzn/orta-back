package org.example.ortaback.exception;

/**
 * Запрос семантически некорректен по бизнес-правилу, которое нельзя выразить
 * bean-валидацией одного поля (например, попытка подписаться на самого себя —
 * ТЗ §4.4).
 *
 * <p>Маппится {@code GlobalExceptionHandler} в {@code 400 Bad Request}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
