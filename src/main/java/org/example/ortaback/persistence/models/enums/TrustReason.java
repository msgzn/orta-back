package org.example.ortaback.persistence.models.enums;

/**
 * Причина изменения {@code User.trustScore} — приклеивается к каждому
 * {@code TrustScoreEvent} для будущих агрегатов («Last 30 days: -10 за no-show,
 * +25 за positive reviews»).
 */
public enum TrustReason {
    PROMOTER_NO_SHOW,
    PARTICIPANT_NO_SHOW,
    POSITIVE_REVIEW,
    NEGATIVE_REVIEW
}
