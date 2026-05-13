package org.example.ortaback.service;

import org.example.ortaback.dto.request.ParticipantReviewRequestDto;
import org.example.ortaback.dto.request.PromoterReviewRequestDto;
import org.example.ortaback.dto.request.VenueRatingRequestDto;
import org.example.ortaback.dto.response.PublicProfileDto;
import org.example.ortaback.dto.response.ReputationDto;
import org.example.ortaback.persistence.models.User;

public interface ReviewService {

    /**
     * Промоутер оценивает участника. Бизнес-правила:
     * <ul>
     *   <li>событие должно быть в статусе {@code DONE};</li>
     *   <li>reviewer — креатор события;</li>
     *   <li>target — участник того же события (любого статуса);</li>
     *   <li>дубликат отзыва на ту же пару → 409.</li>
     * </ul>
     * Trust delta: {@code came=false} → −10; {@code came=true} + positive
     * комментарий → +5; negative → −5; нейтрально/без коммента → 0.
     */
    void reviewParticipant(Long eventId, ParticipantReviewRequestDto request, User reviewer);

    /**
     * Участник оценивает промоутера. Бизнес-правила:
     * <ul>
     *   <li>событие {@code DONE};</li>
     *   <li>reviewer был в составе ({@code EventParticipant.status = JOINED});</li>
     *   <li>дубликат → 409.</li>
     * </ul>
     * Trust delta: {@code came=false} → −20; +5/−5 по комменту.
     */
    void reviewPromoter(Long eventId, PromoterReviewRequestDto request, User reviewer);

    /**
     * Оценка площадки 1..5 — любая сторона. Никаких изменений trust_score.
     */
    void reviewVenue(Long eventId, VenueRatingRequestDto request, User reviewer);

    // ---------------- Чтение ---------------------------------------------

    /** Текущий trust + delta за 30 дней + последние записи истории. */
    ReputationDto getMyReputation(Long userId);

    /** Публичный профиль другого пользователя — для «Check the owner». */
    PublicProfileDto getPublicProfile(Long userId);
}
