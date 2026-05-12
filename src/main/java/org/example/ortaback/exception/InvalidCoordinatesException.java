package org.example.ortaback.exception;

/**
 * Бросается, когда координаты или радиус не проходят программную валидацию
 * (т.е. валидатор Bean Validation не сработал, например значения пришли в
 * сервисный слой не через DTO).
 */
public class InvalidCoordinatesException extends RuntimeException {

    public InvalidCoordinatesException(String message) {
        super(message);
    }
}
