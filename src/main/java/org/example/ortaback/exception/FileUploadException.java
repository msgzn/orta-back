package org.example.ortaback.exception;

/**
 * Ошибка при загрузке/удалении файла в хранилище. Маппится в 400 для
 * клиентских проблем (плохой Content-Type, превышен размер) и 500 для
 * проблем самого S3 (см. {@code GlobalExceptionHandler}).
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
