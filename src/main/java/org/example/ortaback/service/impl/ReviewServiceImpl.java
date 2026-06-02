package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.request.ParticipantReviewRequestDto;
import org.example.ortaback.dto.request.PromoterReviewRequestDto;
import org.example.ortaback.dto.request.VenueRatingRequestDto;
import org.example.ortaback.dto.response.PublicProfileDto;
import org.example.ortaback.dto.response.ReputationDto;
import org.example.ortaback.dto.response.TrustScoreEventDto;
import org.example.ortaback.dto.response.UniversityMiniDto;
import org.example.ortaback.dto.response.UserStatsDto;
import org.example.ortaback.exception.EventStateException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventReview;
import org.example.ortaback.persistence.models.TrustScoreEvent;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.NotificationType;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.ReviewType;
import org.example.ortaback.persistence.models.enums.Sentiment;
import org.example.ortaback.persistence.models.enums.TrustReason;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.persistence.repository.EventReviewRepository;
import org.example.ortaback.persistence.repository.TrustScoreEventRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.NotificationService;
import org.example.ortaback.service.ReviewService;
import org.example.ortaback.service.SentimentAnalyzer;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** Сколько последних записей истории отдаём в {@code ReputationDto}. */
    private static final int HISTORY_PAGE_SIZE = 20;

    /** Трастовые дельты (плюс — поощрение, минус — штраф). */
    static final int DELTA_POSITIVE_REVIEW    =  5;
    static final int DELTA_NEGATIVE_REVIEW    = -5;
    static final int DELTA_PARTICIPANT_NO_SHOW = -10;
    static final int DELTA_PROMOTER_NO_SHOW   = -20;

    /** Пороги уровней доверия (ТЗ §1.5): Свой ≥12, Надёжный ≥30, Эксперт ≥60. */
    private static final int[] TRUST_LEVEL_THRESHOLDS = {12, 30, 60};
    private static final String[] TRUST_LEVEL_NAMES   = {"Свой", "Надёжный", "Эксперт"};

    private final EventRepository eventRepository;
    private final EventReviewRepository reviewRepository;
    private final EventParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TrustScoreEventRepository trustScoreEventRepository;
    private final SentimentAnalyzer sentimentAnalyzer;
    private final NotificationService notificationService;

    // =======================================================================
    //  Write: участник / промоутер / площадка
    // =======================================================================

    @Override
    @Transactional
    public void reviewParticipant(Long eventId,
                                  ParticipantReviewRequestDto request,
                                  User reviewer) {
        Event event = loadDoneEvent(eventId);

        if (!Objects.equals(event.getCreator().getId(), reviewer.getId())) {
            throw new AccessDeniedException(
                    "Only the event creator can review participants");
        }
        User target = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: id=" + request.userId()));

        if (!participantRepository.findByEventIdAndUserId(eventId, target.getId()).isPresent()) {
            throw new EventStateException(
                    "Target user is not a participant of this event");
        }
        if (reviewRepository.existsByEventAndReviewerAndType(
                eventId, reviewer.getId(), target.getId(), ReviewType.PARTICIPANT_BY_PROMOTER)) {
            throw new ResourceAlreadyExistsException(
                    "You already reviewed this participant");
        }

        Sentiment sentiment = sentimentAnalyzer.analyze(request.comment());
        int delta = computeParticipantDelta(request.came(), sentiment);
        TrustReason reason = pickReason(request.came(), sentiment, /*isPromoter*/ false);

        persistReview(event, reviewer, target, ReviewType.PARTICIPANT_BY_PROMOTER,
                request.came(), null, request.comment(), sentiment, delta);

        if (delta != 0 && reason != null) {
            applyTrustChange(target, delta, reason, event);
        }
    }

    @Override
    @Transactional
    public void reviewPromoter(Long eventId,
                               PromoterReviewRequestDto request,
                               User reviewer) {
        Event event = loadDoneEvent(eventId);
        User promoter = event.getCreator();

        if (Objects.equals(promoter.getId(), reviewer.getId())) {
            throw new EventStateException("Cannot review yourself as the promoter");
        }
        // Reviewer должен был быть в составе в роли JOINED.
        if (!participantRepository.existsByEventIdAndUserIdAndStatus(
                eventId, reviewer.getId(), ParticipantStatus.JOINED)) {
            throw new AccessDeniedException(
                    "Only JOINED participants can review the promoter");
        }
        if (reviewRepository.existsByEventAndReviewerAndType(
                eventId, reviewer.getId(), promoter.getId(), ReviewType.PROMOTER_BY_PARTICIPANT)) {
            throw new ResourceAlreadyExistsException(
                    "You already reviewed the promoter of this event");
        }

        Sentiment sentiment = sentimentAnalyzer.analyze(request.comment());
        int delta = computePromoterDelta(request.came(), sentiment);
        TrustReason reason = pickReason(request.came(), sentiment, /*isPromoter*/ true);

        persistReview(event, reviewer, promoter, ReviewType.PROMOTER_BY_PARTICIPANT,
                request.came(), null, request.comment(), sentiment, delta);

        if (delta != 0 && reason != null) {
            applyTrustChange(promoter, delta, reason, event);
        }
    }

    @Override
    @Transactional
    public void reviewVenue(Long eventId, VenueRatingRequestDto request, User reviewer) {
        Event event = loadDoneEvent(eventId);
        // Право оценить площадку — у креатора или JOINED-участника.
        boolean isCreator = Objects.equals(event.getCreator().getId(), reviewer.getId());
        boolean isJoined = participantRepository.existsByEventIdAndUserIdAndStatus(
                eventId, reviewer.getId(), ParticipantStatus.JOINED);
        if (!isCreator && !isJoined) {
            throw new AccessDeniedException(
                    "Only the creator or a JOINED participant can rate the venue");
        }
        if (reviewRepository.existsByEventAndReviewerAndType(
                eventId, reviewer.getId(), null, ReviewType.VENUE)) {
            throw new ResourceAlreadyExistsException(
                    "You already rated this venue");
        }
        persistReview(event, reviewer, null, ReviewType.VENUE,
                null, request.stars(), null, null, /*delta*/ 0);
    }

    // =======================================================================
    //  Read: reputation / public profile
    // =======================================================================

    @Override
    @Transactional(readOnly = true)
    public ReputationDto getMyReputation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + userId));

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Integer delta = trustScoreEventRepository.sumDeltaSince(userId, since);

        List<TrustScoreEventDto> history = trustScoreEventRepository
                .findByUserIdOrderedDesc(userId, PageRequest.of(0, HISTORY_PAGE_SIZE))
                .stream()
                .map(ReviewServiceImpl::toTrustEventDto)
                .toList();

        return new ReputationDto(user.getTrustScore(), delta, history);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfileDto getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + userId));

        UniversityMiniDto universityMini = null;
        University university = user.getUniversity();
        if (university != null) {
            universityMini = new UniversityMiniDto(university.getId(), university.getName());
        }

        List<TrustScoreEventDto> recent = trustScoreEventRepository
                .findByUserIdOrderedDesc(userId, PageRequest.of(0, 10))
                .stream()
                .map(ReviewServiceImpl::toTrustEventDto)
                .toList();

        return new PublicProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getTrustScore(),
                universityMini,
                user.getCreatedAt(),
                recent,
                computeStats(userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: id=" + userId);
        }
        return computeStats(userId);
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    /**
     * Сводные агрегаты профиля (ТЗ §4.5). Чистый расчёт из существующих таблиц —
     * без новых сущностей. См. {@link UserStatsDto} про семантику полей и почему
     * {@code avgRating} всегда {@code null}.
     */
    private UserStatsDto computeStats(Long userId) {
        long created   = eventRepository.countByCreatorId(userId);
        long cancelled = eventRepository.countByCreatorIdAndStatus(userId, EventStatus.CANCELED);
        long joined    = participantRepository.countByUserIdAndStatus(userId, ParticipantStatus.JOINED);
        long attended  = reviewRepository.countParticipantAttendance(userId, true);
        long noShow    = reviewRepository.countParticipantAttendance(userId, false);

        long rated = attended + noShow;
        Double attendanceRate = rated == 0 ? null : (double) attended / rated;

        // avgRating: персонального звёздного рейтинга в модели нет (starRating
        // только у VENUE) → честно null, клиент рисует «—». Детали — javadoc DTO.
        Double avgRating = null;

        return new UserStatsDto(created, joined, attended, cancelled, avgRating, attendanceRate);
    }

    private Event loadDoneEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: id=" + eventId));
        if (event.getStatus() != EventStatus.DONE) {
            throw new EventStateException(
                    "Reviews are only allowed on finished events (status=DONE)");
        }
        return event;
    }

    private void persistReview(Event event, User reviewer, User target, ReviewType type,
                               Boolean came, Integer stars,
                               String comment, Sentiment sentiment, int delta) {
        reviewRepository.save(EventReview.builder()
                .event(event)
                .reviewer(reviewer)
                .target(target)
                .type(type)
                .came(came)
                .starRating(stars)
                .comment(comment)
                .sentiment(sentiment)
                .trustDelta(delta)
                .build());
    }

    /**
     * Применяет изменение к {@code User.trustScore} и пишет историческую
     * запись. Управляемая сущность — пишется в {@code commit} через
     * dirty checking.
     */
    private void applyTrustChange(User target, int delta, TrustReason reason, Event source) {
        float current = target.getTrustScore() != null ? target.getTrustScore() : 0f;
        float updated = current + delta;
        target.setTrustScore(updated);

        trustScoreEventRepository.save(TrustScoreEvent.builder()
                .user(target)
                .delta(delta)
                .reason(reason)
                .event(source)
                .build());

        maybeNotifyTrustLevelUp(target, current, updated, source);
    }

    /**
     * TRUST_LEVEL_UP (ТЗ §4.2): если рост trust пересёк порог уровня вверх —
     * уведомляем пользователя. При пересечении нескольких порогов за один
     * отзыв называем наивысший достигнутый уровень. Понижения и нулевые
     * изменения уведомлений не порождают.
     */
    private void maybeNotifyTrustLevelUp(User target, float oldScore, float newScore, Event source) {
        if (newScore <= oldScore) {
            return;
        }
        String reached = null;
        for (int i = 0; i < TRUST_LEVEL_THRESHOLDS.length; i++) {
            int threshold = TRUST_LEVEL_THRESHOLDS[i];
            if (oldScore < threshold && newScore >= threshold) {
                reached = TRUST_LEVEL_NAMES[i];
            }
        }
        if (reached != null) {
            notificationService.create(
                    target,
                    NotificationType.TRUST_LEVEL_UP,
                    null,
                    source,
                    "Новый уровень доверия: " + reached);
        }
    }

    private static int computeParticipantDelta(Boolean came, Sentiment sentiment) {
        if (Boolean.FALSE.equals(came)) return DELTA_PARTICIPANT_NO_SHOW;
        if (sentiment == Sentiment.POSITIVE) return DELTA_POSITIVE_REVIEW;
        if (sentiment == Sentiment.NEGATIVE) return DELTA_NEGATIVE_REVIEW;
        return 0;
    }

    private static int computePromoterDelta(Boolean came, Sentiment sentiment) {
        if (Boolean.FALSE.equals(came)) return DELTA_PROMOTER_NO_SHOW;
        if (sentiment == Sentiment.POSITIVE) return DELTA_POSITIVE_REVIEW;
        if (sentiment == Sentiment.NEGATIVE) return DELTA_NEGATIVE_REVIEW;
        return 0;
    }

    private static TrustReason pickReason(Boolean came, Sentiment sentiment, boolean isPromoter) {
        if (Boolean.FALSE.equals(came)) {
            return isPromoter ? TrustReason.PROMOTER_NO_SHOW : TrustReason.PARTICIPANT_NO_SHOW;
        }
        if (sentiment == Sentiment.POSITIVE) return TrustReason.POSITIVE_REVIEW;
        if (sentiment == Sentiment.NEGATIVE) return TrustReason.NEGATIVE_REVIEW;
        return null;
    }

    private static TrustScoreEventDto toTrustEventDto(TrustScoreEvent t) {
        Long eventId = t.getEvent() != null ? t.getEvent().getId() : null;
        String eventTitle = t.getEvent() != null ? t.getEvent().getTitle() : null;
        return new TrustScoreEventDto(t.getId(), t.getDelta(), t.getReason(),
                eventId, eventTitle, t.getCreatedAt());
    }
}
