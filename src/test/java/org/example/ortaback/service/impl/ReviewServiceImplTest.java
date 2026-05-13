package org.example.ortaback.service.impl;

import org.example.ortaback.dto.request.ParticipantReviewRequestDto;
import org.example.ortaback.dto.request.PromoterReviewRequestDto;
import org.example.ortaback.dto.request.VenueRatingRequestDto;
import org.example.ortaback.exception.EventStateException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.EventReview;
import org.example.ortaback.persistence.models.TrustScoreEvent;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.ReviewType;
import org.example.ortaback.persistence.models.enums.Sentiment;
import org.example.ortaback.persistence.models.enums.TrustReason;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.example.ortaback.persistence.repository.EventReviewRepository;
import org.example.ortaback.persistence.repository.TrustScoreEventRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.SentimentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventReviewRepository reviewRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock UserRepository userRepository;
    @Mock TrustScoreEventRepository trustScoreEventRepository;
    @Mock SentimentAnalyzer sentimentAnalyzer;

    ReviewServiceImpl reviewService;

    User promoter    = User.builder().id(1L).firstName("Promo").role(UserRole.USER).trustScore(50f).build();
    User participant = User.builder().id(2L).firstName("Part").role(UserRole.USER).trustScore(50f).build();
    Event doneEvent  = Event.builder().id(100L).creator(promoter).status(EventStatus.DONE).title("Match").build();

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                eventRepository, reviewRepository, participantRepository,
                userRepository, trustScoreEventRepository, sentimentAnalyzer);
    }

    // =======================================================================
    //  reviewParticipant — promoter оценивает участника
    // =======================================================================

    @Test
    void reviewParticipant_eventNotDone_throws() {
        Event active = Event.builder().id(100L).creator(promoter).status(EventStatus.ACTIVE).build();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, "great"), promoter))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("DONE");
    }

    @Test
    void reviewParticipant_nonCreator_throwsAccessDenied() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));

        assertThatThrownBy(() -> reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, null), participant))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reviewParticipant_targetNotInEvent_throws() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, null), promoter))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void reviewParticipant_duplicate_throws409() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(participantRepository.findByEventIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(EventParticipant.builder().event(doneEvent).user(participant).status(ParticipantStatus.JOINED).build()));
        when(reviewRepository.existsByEventAndReviewerAndType(
                eq(100L), eq(1L), eq(2L), eq(ReviewType.PARTICIPANT_BY_PROMOTER))).thenReturn(true);

        assertThatThrownBy(() -> reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, null), promoter))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void reviewParticipant_noShow_appliesMinus10ToTarget() {
        stubReviewParticipantFlow();

        reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, false, null), promoter);

        ArgumentCaptor<TrustScoreEvent> tsCaptor = ArgumentCaptor.forClass(TrustScoreEvent.class);
        verify(trustScoreEventRepository).save(tsCaptor.capture());
        assertThat(tsCaptor.getValue().getDelta()).isEqualTo(-10);
        assertThat(tsCaptor.getValue().getReason()).isEqualTo(TrustReason.PARTICIPANT_NO_SHOW);
        assertThat(participant.getTrustScore()).isEqualTo(40f);   // 50 - 10
    }

    @Test
    void reviewParticipant_positiveComment_appliesPlus5() {
        stubReviewParticipantFlow();
        when(sentimentAnalyzer.analyze("great game")).thenReturn(Sentiment.POSITIVE);

        reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, "great game"), promoter);

        verify(trustScoreEventRepository).save(any(TrustScoreEvent.class));
        assertThat(participant.getTrustScore()).isEqualTo(55f);   // 50 + 5
    }

    @Test
    void reviewParticipant_negativeComment_appliesMinus5() {
        stubReviewParticipantFlow();
        when(sentimentAnalyzer.analyze("rude and late")).thenReturn(Sentiment.NEGATIVE);

        reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, "rude and late"), promoter);

        assertThat(participant.getTrustScore()).isEqualTo(45f);   // 50 - 5
    }

    @Test
    void reviewParticipant_came_but_noComment_zeroDelta() {
        stubReviewParticipantFlow();

        reviewService.reviewParticipant(
                100L, new ParticipantReviewRequestDto(2L, true, null), promoter);

        // Никакой записи в историю не должно быть (delta = 0).
        verify(trustScoreEventRepository, never()).save(any());
        assertThat(participant.getTrustScore()).isEqualTo(50f);
    }

    // =======================================================================
    //  reviewPromoter — участник оценивает промоутера
    // =======================================================================

    @Test
    void reviewPromoter_promoterCannotReviewHimself_throws() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));

        assertThatThrownBy(() -> reviewService.reviewPromoter(
                100L, new PromoterReviewRequestDto(true, null), promoter))
                .isInstanceOf(EventStateException.class);
    }

    @Test
    void reviewPromoter_notJoined_throwsAccessDenied() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(participantRepository.existsByEventIdAndUserIdAndStatus(100L, 2L, ParticipantStatus.JOINED))
                .thenReturn(false);

        assertThatThrownBy(() -> reviewService.reviewPromoter(
                100L, new PromoterReviewRequestDto(true, null), participant))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void reviewPromoter_promoterNoShow_appliesMinus20() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(participantRepository.existsByEventIdAndUserIdAndStatus(100L, 2L, ParticipantStatus.JOINED))
                .thenReturn(true);
        when(reviewRepository.existsByEventAndReviewerAndType(anyLong(), anyLong(), any(), any()))
                .thenReturn(false);

        reviewService.reviewPromoter(
                100L, new PromoterReviewRequestDto(false, null), participant);

        ArgumentCaptor<TrustScoreEvent> tsCaptor = ArgumentCaptor.forClass(TrustScoreEvent.class);
        verify(trustScoreEventRepository).save(tsCaptor.capture());
        assertThat(tsCaptor.getValue().getDelta()).isEqualTo(-20);
        assertThat(tsCaptor.getValue().getReason()).isEqualTo(TrustReason.PROMOTER_NO_SHOW);
        assertThat(promoter.getTrustScore()).isEqualTo(30f);   // 50 - 20
    }

    // =======================================================================
    //  reviewVenue
    // =======================================================================

    @Test
    void reviewVenue_creator_allowedNoTrustChange() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(reviewRepository.existsByEventAndReviewerAndType(anyLong(), anyLong(), any(), any()))
                .thenReturn(false);

        reviewService.reviewVenue(100L, new VenueRatingRequestDto(5), promoter);

        ArgumentCaptor<EventReview> captor = ArgumentCaptor.forClass(EventReview.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStarRating()).isEqualTo(5);
        assertThat(captor.getValue().getType()).isEqualTo(ReviewType.VENUE);
        verify(trustScoreEventRepository, never()).save(any());
    }

    @Test
    void reviewVenue_outsider_throwsAccessDenied() {
        User outsider = User.builder().id(99L).role(UserRole.USER).build();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(participantRepository.existsByEventIdAndUserIdAndStatus(100L, 99L, ParticipantStatus.JOINED))
                .thenReturn(false);

        assertThatThrownBy(() -> reviewService.reviewVenue(100L, new VenueRatingRequestDto(5), outsider))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private void stubReviewParticipantFlow() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(doneEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(participantRepository.findByEventIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(EventParticipant.builder()
                        .event(doneEvent).user(participant).status(ParticipantStatus.JOINED).build()));
        when(reviewRepository.existsByEventAndReviewerAndType(
                eq(100L), eq(1L), eq(2L), eq(ReviewType.PARTICIPANT_BY_PROMOTER))).thenReturn(false);
    }
}
