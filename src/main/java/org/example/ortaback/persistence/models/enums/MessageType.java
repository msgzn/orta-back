package org.example.ortaback.persistence.models.enums;

/**
 * Тип сообщения чата события (ТЗ §4.1).
 *
 * <ul>
 *   <li>{@link #USER} — обычное сообщение участника ({@code sender} не null);</li>
 *   <li>{@link #SYSTEM} — служебное событие беседы (join/leave/start/finish/cancel),
 *       генерируется сервером, {@code sender == null}.</li>
 * </ul>
 */
public enum MessageType {
    USER,
    SYSTEM
}
