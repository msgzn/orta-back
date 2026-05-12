package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.geo.EventDetailDto;
import org.example.ortaback.dto.request.EventCreateRequestDto;
import org.example.ortaback.dto.request.EventUpdateRequestDto;
import org.example.ortaback.dto.response.ParticipantResponseDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "CRUD событий и участие")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Создать событие — creator становится JOINED-участником")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDetailDto create(@Valid @RequestBody EventCreateRequestDto request,
                                 @AuthenticationPrincipal User currentUser) {
        return eventService.createEvent(request, currentUser);
    }

    @Operation(summary = "Карточка события (с маскировкой APPROXIMATE-координат)")
    @GetMapping("/{id}")
    public EventDetailDto get(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser) {
        return eventService.getEvent(id, currentUser);
    }

    @Operation(summary = "Частичное обновление события (только creator или ADMIN)")
    @PatchMapping("/{id}")
    public EventDetailDto update(@PathVariable Long id,
                                 @Valid @RequestBody EventUpdateRequestDto request,
                                 @AuthenticationPrincipal User currentUser) {
        return eventService.updateEvent(id, request, currentUser);
    }

    @Operation(summary = "Отмена события (soft) — status=CANCELED. creator или ADMIN")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id,
                       @AuthenticationPrincipal User currentUser) {
        eventService.cancelEvent(id, currentUser);
    }

    @Operation(summary = "Присоединиться к событию")
    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable Long id,
                     @AuthenticationPrincipal User currentUser) {
        eventService.joinEvent(id, currentUser);
    }

    @Operation(summary = "Покинуть событие (creator не может — должен cancel)")
    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id,
                      @AuthenticationPrincipal User currentUser) {
        eventService.leaveEvent(id, currentUser);
    }

    @Operation(summary = "Список участников события")
    @GetMapping("/{id}/participants")
    public List<ParticipantResponseDto> participants(@PathVariable Long id,
                                                     @AuthenticationPrincipal User currentUser) {
        return eventService.listParticipants(id, currentUser);
    }
}
