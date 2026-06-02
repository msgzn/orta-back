package org.example.ortaback.persistence.models.enums;

/**
 * Тип in-app уведомления (ТЗ §4.2). Фронт мапит тип на иконку и на действие
 * по тапу (deep-link в событие, в профиль, в экран Rate и т.д.).
 *
 * <ul>
 *   <li>{@link #EVENT_JOIN} — кто-то присоединился к моему событию
 *       (recipient = креатор, actor = присоединившийся);</li>
 *   <li>{@link #EVENT_REMINDER} — напоминание участнику, что событие скоро
 *       начнётся (генерится планировщиком);</li>
 *   <li>{@link #RATE_PROMPT} — событие завершено, пора оценить
 *       участников/промоутера;</li>
 *   <li>{@link #TRUST_LEVEL_UP} — пользователь перешёл на следующий уровень
 *       доверия (ТЗ §1.5: Свой ≥12, Надёжный ≥30, Эксперт ≥60);</li>
 *   <li>{@link #EVENT_INVITE} — приглашение в событие (требует соц-графа,
 *       реализуется в v2 — пока не генерится);</li>
 *   <li>{@link #NEARBY_DIGEST} — дайджест «события рядом» (требует решения по
 *       геокадансу — пока не генерится).</li>
 * </ul>
 */
public enum NotificationType {
    EVENT_JOIN,
    EVENT_REMINDER,
    RATE_PROMPT,
    EVENT_INVITE,
    TRUST_LEVEL_UP,
    NEARBY_DIGEST
}
