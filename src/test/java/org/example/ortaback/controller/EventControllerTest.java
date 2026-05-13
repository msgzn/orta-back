package org.example.ortaback.controller;

import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.request.EventCreateRequestDto;
import org.example.ortaback.dto.request.EventUpdateRequestDto;
import org.example.ortaback.dto.response.CreatorMiniDto;
import org.example.ortaback.dto.response.EventFeedItemDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.ParticipantResponseDto;
import org.example.ortaback.exception.EventStateException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.EventStatus;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.ParticipantStatus;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.service.EventService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest extends MockMvcTestSupport {

    private final EventService eventService = mock(EventService.class);
    private final org.example.ortaback.service.ReviewService reviewService =
            mock(org.example.ortaback.service.ReviewService.class);
    private MockMvc mvc;

    private final User currentUser = User.builder()
            .id(1L).email("u@example.com")
            .firstName("U").lastName("U")
            .hashedPassword("BCRYPT").role(UserRole.USER)
            .build();

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new EventController(eventService, reviewService));
        authenticate(currentUser);
    }

    private EventDetailDto stubDetail() {
        return new EventDetailDto(
                100L, "Match", "desc", "Gym", "Almaty",
                43.22, 76.85,
                LocalDateTime.of(2026, 6, 1, 18, 0), 90, 2, 10, null,
                EventStatus.ACTIVE, LocationPrecision.EXACT,
                10L, "Football", null,
                1L, "U", "U", null,
                1L);
    }

    // -----------------------------------------------------------------------
    //  /events (feed)
    // -----------------------------------------------------------------------

    @Test
    void feed_recent_returnsPage() throws Exception {
        EventFeedItemDto item = new EventFeedItemDto(
                100L, "Match", "Gym", "Almaty",
                43.22, 76.85, LocationPrecision.EXACT,
                LocalDateTime.of(2026, 6, 1, 18, 0), 90, 10, 1L, null, EventStatus.ACTIVE,
                10L, "Football", null,
                new CreatorMiniDto(1L, "U", "U", null, 0.0f));
        PageDto<EventFeedItemDto> page = new PageDto<>(List.of(item), 0, 20, 1, 1, false, false);
        when(eventService.findFeed(eq(10L), eq(0), eq(20), any())).thenReturn(page);

        mvc.perform(get("/api/v1/events")
                        .param("categoryId", "10").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Match"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void feed_recommended_delegatesToRecommendedMethod() throws Exception {
        when(eventService.findRecommended(any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageDto<>(List.of(), 0, 20, 0, 0, false, false));

        mvc.perform(get("/api/v1/events").param("sort", "recommended"))
                .andExpect(status().isOk());

        verify(eventService).findRecommended(eq(null), eq(0), eq(20), any());
    }

    // -----------------------------------------------------------------------
    //  POST /events
    // -----------------------------------------------------------------------

    @Test
    void create_validBody_returns201() throws Exception {
        when(eventService.createEvent(any(), any())).thenReturn(stubDetail());

        EventCreateRequestDto body = new EventCreateRequestDto(
                "Match", "desc", "Gym", "Almaty",
                43.22, 76.85, LocationPrecision.EXACT,
                10L,
                LocalDateTime.now().plusDays(2), 90, 2, 10, null);

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.title").value("Match"));
    }

    @Test
    void create_titleBlank_returns400() throws Exception {
        EventCreateRequestDto bad = new EventCreateRequestDto(
                "", null, null, null, 43.22, 76.85, null, 10L,
                LocalDateTime.now().plusDays(2), null, null, null, null);

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    void create_startTimeInPast_returns400() throws Exception {
        EventCreateRequestDto bad = new EventCreateRequestDto(
                "Match", null, null, null, 43.22, 76.85, null, 10L,
                LocalDateTime.now().minusDays(1), null, null, null, null);

        mvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.startTime").exists());
    }

    // -----------------------------------------------------------------------
    //  GET /events/{id}
    // -----------------------------------------------------------------------

    @Test
    void get_returnsDetail() throws Exception {
        when(eventService.getEvent(eq(100L), any())).thenReturn(stubDetail());

        mvc.perform(get("/api/v1/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(eventService.getEvent(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Event not found: id=999"));

        mvc.perform(get("/api/v1/events/999"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    //  PATCH / DELETE
    // -----------------------------------------------------------------------

    @Test
    void update_creatorAllowed_returnsUpdatedDetail() throws Exception {
        when(eventService.updateEvent(eq(100L), any(), any())).thenReturn(stubDetail());

        EventUpdateRequestDto patch = new EventUpdateRequestDto(
                "New title", null, null, null, null, null, null, null, null, null, null, null, null, null);

        mvc.perform(patch("/api/v1/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk());
    }

    @Test
    void update_nonCreator_returns403() throws Exception {
        when(eventService.updateEvent(eq(100L), any(), any()))
                .thenThrow(new AccessDeniedException("Only the event creator or an admin"));

        mvc.perform(patch("/api/v1/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_returns204() throws Exception {
        mvc.perform(delete("/api/v1/events/100"))
                .andExpect(status().isNoContent());

        verify(eventService).cancelEvent(eq(100L), any());
    }

    // -----------------------------------------------------------------------
    //  Участие
    // -----------------------------------------------------------------------

    @Test
    void join_returns204() throws Exception {
        mvc.perform(post("/api/v1/events/100/join"))
                .andExpect(status().isNoContent());

        verify(eventService).joinEvent(eq(100L), any());
    }

    @Test
    void join_eventFull_returns409() throws Exception {
        doThrow(new EventStateException("Event is full"))
                .when(eventService).joinEvent(eq(100L), any());

        mvc.perform(post("/api/v1/events/100/join"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event is full"));
    }

    @Test
    void leave_returns204() throws Exception {
        mvc.perform(delete("/api/v1/events/100/leave"))
                .andExpect(status().isNoContent());

        verify(eventService).leaveEvent(eq(100L), any());
    }

    @Test
    void leave_creatorCannotLeave_returns409() throws Exception {
        doThrow(new EventStateException("creator cannot leave"))
                .when(eventService).leaveEvent(eq(100L), any());

        mvc.perform(delete("/api/v1/events/100/leave"))
                .andExpect(status().isConflict());
    }

    @Test
    void listParticipants_returnsArray() throws Exception {
        when(eventService.listParticipants(eq(100L), any())).thenReturn(List.of(
                new ParticipantResponseDto(2L, "Bob", "B", null,
                        ParticipantStatus.JOINED, LocalDateTime.now(), null)));

        mvc.perform(get("/api/v1/events/100/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Bob"))
                .andExpect(jsonPath("$[0].status").value("JOINED"));
    }
}
