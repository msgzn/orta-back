package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.request.MarkReadRequestDto;
import org.example.ortaback.dto.request.SendMessageRequestDto;
import org.example.ortaback.dto.response.ConversationSummaryDto;
import org.example.ortaback.dto.response.EventMessageDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Чат события (ТЗ §4.1). Беседа = событие; доступ — создатель и
 * {@code JOINED}-участники (проверяется в сервисе → 403 для прочих). Все
 * эндпоинты требуют JWT.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Лента сообщений беседы: before — старше курсора, after — опрос новее, иначе последние")
    @GetMapping("/events/{id}/messages")
    public PageDto<EventMessageDto> listMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal User currentUser) {
        return chatService.listMessages(id, before, after, size, currentUser);
    }

    @Operation(summary = "Отправить сообщение в беседу события")
    @PostMapping("/events/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public EventMessageDto sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        return chatService.sendMessage(id, request.text(), currentUser);
    }

    @Operation(summary = "Отметить сообщения беседы прочитанными до указанного id (идемпотентно)")
    @PostMapping("/events/{id}/messages/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @PathVariable Long id,
            @Valid @RequestBody MarkReadRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        chatService.markRead(id, request.lastMessageId(), currentUser);
    }

    @Operation(summary = "Список бесед текущего пользователя (события, где он участник/создатель)")
    @GetMapping("/me/conversations")
    public List<ConversationSummaryDto> myConversations(@AuthenticationPrincipal User currentUser) {
        return chatService.listMyConversations(currentUser);
    }
}
