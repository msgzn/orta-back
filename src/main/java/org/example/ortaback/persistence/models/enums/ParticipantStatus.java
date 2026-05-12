package org.example.ortaback.persistence.models.enums;

/**
 * Статус участия пользователя в событии.
 *
 * <ul>
 *   <li>{@link #JOINED} — пользователь присоединился и числится в составе.</li>
 *   <li>{@link #KICKED} — удалён создателем события.</li>
 *   <li>{@link #LEFT} — добровольно вышел.</li>
 * </ul>
 */
public enum ParticipantStatus {
    JOINED,
    KICKED,
    LEFT
}
