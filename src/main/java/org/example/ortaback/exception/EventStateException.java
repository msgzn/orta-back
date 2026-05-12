package org.example.ortaback.exception;

/**
 * Бизнес-правило события нарушено: попытка вступить в полное / отменённое
 * событие, повторное присоединение, выход создателя и т.п.
 *
 * <p>Маппится {@code GlobalExceptionHandler} в {@code 409 Conflict}.
 */
public class EventStateException extends RuntimeException {

    public EventStateException(String message) {
        super(message);
    }
}
