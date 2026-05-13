package org.example.ortaback.service.impl;

import org.example.ortaback.persistence.models.enums.Sentiment;
import org.example.ortaback.service.SentimentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Keyword-based MVP-стаб {@link SentimentAnalyzer}'а.
 *
 * <p>Считает положительные/отрицательные слова в тексте (RU + EN) и возвращает
 * победителя; ничья / отсутствие маркеров → {@link Sentiment#NEUTRAL}.
 *
 * <p>Перенесём на нормальный NLP, когда подключим внешний API. Контракт
 * интерфейса не меняется — замена реализации = подмена бина.
 */
@Service
public class KeywordSentimentAnalyzer implements SentimentAnalyzer {

    private static final Set<String> POSITIVE_WORDS = Set.of(
            // Russian
            "круто", "отлично", "супер", "класс", "хорошо", "прекрасно",
            "понравилось", "спасибо", "топ", "отличная", "молодец",
            // English
            "great", "good", "awesome", "nice", "excellent", "perfect",
            "love", "loved", "best", "amazing", "thanks"
    );

    private static final Set<String> NEGATIVE_WORDS = Set.of(
            // Russian
            "плохо", "ужас", "ужасно", "отвратительно", "никогда", "опоздал",
            "опоздала", "грубый", "груб", "хамство", "разочарован", "разочарована",
            "обман",
            // English
            "bad", "awful", "terrible", "rude", "late", "worst", "hate",
            "scam", "fake", "disappointed", "boring"
    );

    @Override
    public Sentiment analyze(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // (?U) включает Unicode-aware режим — без него кириллица матчится
        // \W и пропадает целиком на split'е.
        String[] tokens = text.toLowerCase(Locale.ROOT).split("(?U)\\W+");
        int positives = 0;
        int negatives = 0;
        for (String token : tokens) {
            if (POSITIVE_WORDS.contains(token)) positives++;
            else if (NEGATIVE_WORDS.contains(token)) negatives++;
        }
        if (positives > negatives) return Sentiment.POSITIVE;
        if (negatives > positives) return Sentiment.NEGATIVE;
        return Sentiment.NEUTRAL;
    }
}
