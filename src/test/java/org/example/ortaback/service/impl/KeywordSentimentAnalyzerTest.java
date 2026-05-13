package org.example.ortaback.service.impl;

import org.example.ortaback.persistence.models.enums.Sentiment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordSentimentAnalyzerTest {

    private final KeywordSentimentAnalyzer analyzer = new KeywordSentimentAnalyzer();

    @Test
    void null_returnsNull() {
        assertThat(analyzer.analyze(null)).isNull();
    }

    @Test
    void blank_returnsNull() {
        assertThat(analyzer.analyze("   ")).isNull();
    }

    @Test
    void positiveRussian_detected() {
        assertThat(analyzer.analyze("Было круто и весело, спасибо!"))
                .isEqualTo(Sentiment.POSITIVE);
    }

    @Test
    void negativeEnglish_detected() {
        assertThat(analyzer.analyze("Promoter was rude and late"))
                .isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    void mixedTie_returnsNeutral() {
        // одно good, одно bad — ничья → NEUTRAL
        assertThat(analyzer.analyze("good and bad"))
                .isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    void unrelated_returnsNeutral() {
        assertThat(analyzer.analyze("Просто пришёл и ушёл"))
                .isEqualTo(Sentiment.NEUTRAL);
    }

    @Test
    void caseInsensitive() {
        assertThat(analyzer.analyze("GREAT TIME!"))
                .isEqualTo(Sentiment.POSITIVE);
    }
}
