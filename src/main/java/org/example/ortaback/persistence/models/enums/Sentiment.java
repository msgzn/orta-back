package org.example.ortaback.persistence.models.enums;

/**
 * Тональность текстового отзыва, посчитанная {@code SentimentAnalyzer}'ом.
 * {@code null} — отзыв без текста (только came + (опц.) звёзды).
 */
public enum Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}
