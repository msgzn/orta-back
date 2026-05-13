package org.example.ortaback.persistence.models.enums;

/**
 * Кто кого оценивает после завершения события.
 *
 * <ul>
 *   <li>{@link #PARTICIPANT_BY_PROMOTER} — промоутер ставит оценку участнику
 *       (came + опциональный коммент).</li>
 *   <li>{@link #PROMOTER_BY_PARTICIPANT} — участник ставит оценку промоутеру.</li>
 *   <li>{@link #VENUE} — любая сторона оценивает площадку события (1–5 ⭐).</li>
 * </ul>
 */
public enum ReviewType {
    PARTICIPANT_BY_PROMOTER,
    PROMOTER_BY_PARTICIPANT,
    VENUE
}
