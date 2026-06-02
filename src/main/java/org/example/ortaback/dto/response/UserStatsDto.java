package org.example.ortaback.dto.response;

/**
 * Агрегаты профиля (ТЗ §4.5) — сводка активности пользователя как организатора
 * и как участника. Отдаётся на {@code GET /users/me/stats} и вкладывается в
 * {@link PublicProfileDto} («Check the owner»).
 *
 * <p>Считается на лету из существующих данных — БЕЗ отдельной таблицы-агрегата:
 * {@code Event} (создатель/статус), {@code EventParticipant} (членство) и
 * {@code EventReview} (отметки присутствия промоутером). Значения всегда
 * «живые», без риска рассинхрона.
 *
 * <ul>
 *   <li>{@code eventsCreated} — сколько событий пользователь создал (любой статус).</li>
 *   <li>{@code eventsJoined} — в скольких событиях он сейчас числится участником
 *       ({@code ParticipantStatus.JOINED}).</li>
 *   <li>{@code eventsAttended} — на скольких событиях промоутер отметил приход
 *       ({@code came = true}). Единственный достоверный источник факта
 *       присутствия — отдельного статуса «attended» у участника в модели нет.</li>
 *   <li>{@code eventsCancelled} — сколько <b>созданных пользователем</b> событий
 *       завершились отменой ({@code EventStatus.CANCELED}). Сигнал надёжности
 *       организатора. Это НЕ «вышел из чужого события» — добровольный выход
 *       ({@code LEFT}) отменой не считается.</li>
 *   <li>{@code avgRating} — средний звёздный рейтинг <b>пользователя</b>. В
 *       текущей модели его нет: {@code starRating} (1..5) проставляется только
 *       VENUE-отзыву (оценка площадки), а отзывы на человека несут
 *       came/comment/sentiment без числовых звёзд. Поэтому поле всегда
 *       {@code null} — клиент рисует «—». Репутация человека выражается trust
 *       score ({@code ReputationDto}), а не звёздами; рейтинг площадки в
 *       персональный рейтинг НЕ превращаем (no-mock).</li>
 *   <li>{@code attendanceRate} — доля прихода в диапазоне {@code [0..1]} среди
 *       событий, где промоутер отметил присутствие:
 *       {@code attended / (attended + noShow)}. {@code null}, если таких отметок
 *       ещё не было (нет базы для расчёта — не «0%»).</li>
 * </ul>
 */
public record UserStatsDto(
        long eventsCreated,
        long eventsJoined,
        long eventsAttended,
        long eventsCancelled,
        Double avgRating,
        Double attendanceRate
) {}
