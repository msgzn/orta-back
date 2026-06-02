package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.FollowStatusDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.FollowService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Соц-граф подписок (ТЗ §4.4 «Друзья», v2). Выделен в отдельный контроллер
 * под общим базовым путём {@code /api/v1/users} — аналогично тому, как чат
 * получил собственный {@code ChatController}.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Follows")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "Подписаться на пользователя (идемпотентно)")
    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        followService.follow(id, currentUser);
    }

    @Operation(summary = "Отписаться от пользователя (идемпотентно)")
    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        followService.unfollow(id, currentUser);
    }

    @Operation(summary = "Подписчики пользователя («кто на него подписан»)")
    @GetMapping("/{id}/followers")
    public PageDto<UserMiniDto> followers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.listFollowers(id, page, size);
    }

    @Operation(summary = "Подписки пользователя («на кого он подписан»)")
    @GetMapping("/{id}/following")
    public PageDto<UserMiniDto> following(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return followService.listFollowing(id, page, size);
    }

    @Operation(summary = "Статус подписки текущего пользователя на целевого + счётчики")
    @GetMapping("/{id}/follow-status")
    public FollowStatusDto followStatus(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        return followService.getFollowStatus(id, currentUser);
    }
}
