package org.example.ortaback.service;

import org.example.ortaback.persistence.models.enums.Sentiment;

/**
 * Тональность комментария. Контракт стабильный: за интерфейсом сейчас стоит
 * keyword-based стаб, в будущем — внешний NLP (OpenAI / HF / локальная модель).
 */
public interface SentimentAnalyzer {

    /**
     * @return {@link Sentiment} или {@code null} для пустого/{@code null}-текста.
     */
    Sentiment analyze(String text);
}
