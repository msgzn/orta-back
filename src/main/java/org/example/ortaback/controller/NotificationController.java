package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.NotificationDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UnreadCountDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-app уведомления (ТЗ §4.2). Все эндпоинты требуют JWT и работают только с
 * уведомлениями текущего пользователя (recipient = {@code currentUser}).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Лента уведомлений текущего пользователя (новые сверху)")
    @GetMapping
    public PageDto<NotificationDto> myNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return notificationService.listMyNotifications(currentUser.getId(), page, size);
    }

    @Operation(summary = "Количество непрочитанных уведомлений (бейдж на колокольчике)")
    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount(@AuthenticationPrincipal User currentUser) {
        return notificationService.getUnreadCount(currentUser.getId());
    }

    @Operation(summary = "Отметить одно уведомление прочитанным (идемпотентно)")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        notificationService.markRead(currentUser.getId(), id);
    }

    @Operation(summary = "Отметить все уведомления прочитанными")
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllRead(currentUser.getId());
    }
}
