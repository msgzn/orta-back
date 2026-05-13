package org.example.ortaback.service.impl;

import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.request.EventCreateRequestDto;
import org.example.ortaback.dto.request.EventUpdateRequestDto;
import org.example.ortaback.exception.EventStateException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.geo.CoordinateValidator;
import org.example.ortaback.geo.PrivacyMasker;
import org.example.ortaback.mapper.EventMapMapper;
import org.example.ortaback.mapper.ParticipantMapper;
import org.example.ortaback.persistence.models.Category;
import org.example.ortaback.persistence.models.Event;
import org.example.ortaback.persistence.models.EventParticipant;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.repository.CategoryRepository;
import org.example.ortaback.persistence.repository.EventParticipantRepository;
import org.example.ortaback.persistence.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock org.example.ortaback.persistence.repository.EventObservationRepository observationRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CoordinateValidator coordinateValidator;
    @Mock PrivacyMasker privacyMasker;
    @Mock org.example.ortaback.config.GeoProperties geoProperties;
    @Mock EventMapMapper mapper;
    @Mock ParticipantMapper participantMapper;
    @Mock org.example.ortaback.storage.StorageService storageService;

    EventServiceImpl eventService;

    User creator = User.builder().id(1L).firstName("Creator").role(UserRole.USER).build();
    User other   = User.builder().id(2L).firstName("Other").role(UserRole.USER).build();
    User admin   = User.builder().id(99L).firstName("Admin").role(UserRole.ADMIN).build();
    Category category = Category.builder().id(10L).name("Football").build();

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository, participantRepository, observationRepository,
                categoryRepository, coordinateValidator, privacyMasker,
                geoProperties, mapper, participantMapper, storageService);
    }

    private Event activeEvent(int max) {
        return Event.builder()
                .id(100L)
                .creator(creator)
                .category(category)
                .status(EventStatus.ACTIVE)
                .title("Match")
                .startTime(LocalDateTime.now().plusDays(1))
                .maxParticipants(max)
                .locationPrecision(LocationPrecision.EXACT)
                .build();
    }

    // =======================================================================
    //  createEvent
    // =======================================================================

    @Test
    void createEvent_persistsEventAndAutoJoinsCreator() {
        EventCreateRequestDto request = new EventCreateRequestDto(
                "Match", "desc", "Gym KazNU", "Almaty",
                43.22, 76.85, LocationPrecision.EXACT,
                10L,
                LocalDateTime.now().plusDays(2), 90,
                2, 10, null);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });
        when(mapper.toDetail(any(), any(), eq(1L))).thenReturn(mockDetail());

        EventDetailDto detail = eventService.createEvent(request, creator);

        assertThat(detail).isNotNull();
        verify(eventRepository).save(any(Event.class));
        verify(participantRepository).save(any(EventParticipant.class));
        verify(coordinateValidator).validateLatLng(43.22, 76.85);
    }

    @Test
    void createEvent_categoryNotFound_throws() {
        EventCreateRequestDto request = new EventCreateRequestDto(
                "Match", null, null, null, 43.22, 76.85, null,
                999L, LocalDateTime.now().plusDays(1), null, null, null, null);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request, creator))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    void createEvent_minGreaterThanMax_throwsEventState() {
        EventCreateRequestDto request = new EventCreateRequestDto(
                "Match", null, null, null, 43.22, 76.85, null, 10L,
                LocalDateTime.now().plusDays(1), null, 5, 2, null);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> eventService.createEvent(request, creator))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("minParticipants");
    }

    // =======================================================================
    //  updateEvent / cancelEvent — authorization
    // =======================================================================

    @Test
    void updateEvent_nonCreatorNonAdmin_throwsAccessDenied() {
        Event event = activeEvent(10);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        EventUpdateRequestDto request = new EventUpdateRequestDto(
                "Hacked", null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> eventService.updateEvent(100L, request, other))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateEvent_creatorAllowed() {
        Event event = activeEvent(10);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(mapper.toDetail(any(), any(), anyLong())).thenReturn(mockDetail());

        EventUpdateRequestDto request = new EventUpdateRequestDto(
                "New title", null, null, null, null, null, null, null, null, null, null, null, null, null);

        eventService.updateEvent(100L, request, creator);

        assertThat(event.getTitle()).isEqualTo("New title");
    }

    @Test
    void updateEvent_adminAllowedEvenIfNotCreator() {
        Event event = activeEvent(10);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(mapper.toDetail(any(), any(), anyLong())).thenReturn(mockDetail());

        EventUpdateRequestDto request = new EventUpdateRequestDto(
                null, null, null, null, null, null, null, null, null, null, null, null, null, EventStatus.DONE);

        eventService.updateEvent(100L, request, admin);

        assertThat(event.getStatus()).isEqualTo(EventStatus.DONE);
    }

    @Test
    void cancelEvent_idempotentOnAlreadyCancelled() {
        Event event = activeEvent(10);
        event.setStatus(EventStatus.CANCELED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        eventService.cancelEvent(100L, creator);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELED);
    }

    @Test
    void cancelEvent_softCancelsActiveEvent() {
        Event event = activeEvent(10);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        eventService.cancelEvent(100L, creator);

        assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELED);
    }

    // =======================================================================
    //  joinEvent
    // =======================================================================

    @Test
    void joinEvent_cancelledEvent_throwsEventState() {
        Event event = activeEvent(10);
        event.setStatus(EventStatus.CANCELED);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.joinEvent(100L, other))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("Cannot join");
    }

    @Test
    void joinEvent_alreadyJoined_throwsResourceAlreadyExists() {
        Event event = activeEvent(10);
        EventParticipant existing = EventParticipant.builder()
                .event(event).user(other).status(ParticipantStatus.JOINED).build();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.joinEvent(100L, other))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void joinEvent_kicked_cannotRejoin() {
        Event event = activeEvent(10);
        EventParticipant kicked = EventParticipant.builder()
                .event(event).user(other).status(ParticipantStatus.KICKED).build();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.of(kicked));

        assertThatThrownBy(() -> eventService.joinEvent(100L, other))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("removed");
    }

    @Test
    void joinEvent_leftCanRejoin_setsStatusBackToJoined() {
        Event event = activeEvent(10);
        EventParticipant left = EventParticipant.builder()
                .event(event).user(other).status(ParticipantStatus.LEFT).build();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.of(left));
        when(participantRepository.countByEventIdAndStatus(100L, ParticipantStatus.JOINED)).thenReturn(0L);

        eventService.joinEvent(100L, other);

        assertThat(left.getStatus()).isEqualTo(ParticipantStatus.JOINED);
    }

    @Test
    void joinEvent_atCapacity_throwsEventState() {
        Event event = activeEvent(5);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByEventIdAndStatus(100L, ParticipantStatus.JOINED)).thenReturn(5L);

        assertThatThrownBy(() -> eventService.joinEvent(100L, other))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("full");
    }

    @Test
    void joinEvent_unlimitedCapacity_alwaysAllowed() {
        Event event = activeEvent(0);
        event.setMaxParticipants(null);   // не лимитировано
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        eventService.joinEvent(100L, other);

        ArgumentCaptor<EventParticipant> captor = ArgumentCaptor.forClass(EventParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ParticipantStatus.JOINED);
        verify(participantRepository, never()).countByEventIdAndStatus(anyLong(), any());
    }

    // =======================================================================
    //  leaveEvent
    // =======================================================================

    @Test
    void leaveEvent_creatorCannotLeaveOwnEvent() {
        Event event = activeEvent(10);
        EventParticipant creatorParticipation = EventParticipant.builder()
                .event(event).user(creator).status(ParticipantStatus.JOINED).build();
        when(participantRepository.findByEventIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(creatorParticipation));

        assertThatThrownBy(() -> eventService.leaveEvent(100L, creator))
                .isInstanceOf(EventStateException.class)
                .hasMessageContaining("creator");
    }

    @Test
    void leaveEvent_regularParticipant_setsStatusLeft() {
        Event event = activeEvent(10);
        EventParticipant joinedOther = EventParticipant.builder()
                .event(event).user(other).status(ParticipantStatus.JOINED).build();
        when(participantRepository.findByEventIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(joinedOther));

        eventService.leaveEvent(100L, other);

        assertThat(joinedOther.getStatus()).isEqualTo(ParticipantStatus.LEFT);
    }

    @Test
    void leaveEvent_notAParticipant_throwsResourceNotFound() {
        when(participantRepository.findByEventIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.leaveEvent(100L, other))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =======================================================================
    //  listParticipants
    // =======================================================================

    @Test
    void listParticipants_eventNotFound_throws() {
        when(eventRepository.existsById(100L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.listParticipants(100L, creator))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listParticipants_returnsAllRows() {
        Event event = activeEvent(10);
        EventParticipant p = EventParticipant.builder()
                .event(event).user(other).status(ParticipantStatus.JOINED).build();
        when(eventRepository.existsById(100L)).thenReturn(true);
        when(participantRepository.findByEventId(100L)).thenReturn(List.of(p));
        when(participantMapper.toDto(p)).thenReturn(
                new org.example.ortaback.dto.response.ParticipantResponseDto(
                        2L, "Other", null, null, ParticipantStatus.JOINED, null, null));

        var result = eventService.listParticipants(100L, creator);
        assertThat(result).hasSize(1);
        verify(participantMapper, times(1)).toDto(p);
    }

    // =======================================================================
    //  Helpers
    // =======================================================================

    private static EventDetailDto mockDetail() {
        return new EventDetailDto(
                100L, "Match", null, null, null,
                43.22, 76.85,
                LocalDateTime.now().plusDays(1), null, null, null, null,
                EventStatus.ACTIVE, LocationPrecision.EXACT,
                10L, "Football", null,
                1L, "Creator", null, null,
                1L);
    }
}
