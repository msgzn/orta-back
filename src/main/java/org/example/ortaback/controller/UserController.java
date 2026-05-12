package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.geo.LocationUpdateRequestDto;
import org.example.ortaback.dto.request.InterestRequestDto;
import org.example.ortaback.dto.request.UserProfileUpdateRequestDto;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.UserProfileDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    // -----------------------------------------------------------------------
    // Профиль
    // -----------------------------------------------------------------------

    @Operation(summary = "Текущий профиль пользователя со списком интересов")
    @GetMapping("/me")
    public UserProfileDto getMyProfile(@AuthenticationPrincipal User currentUser) {
        return userService.getProfile(currentUser.getId());
    }

    @Operation(summary = "Частичное обновление профиля")
    @PatchMapping("/me")
    public UserProfileDto updateMyProfile(@Valid @RequestBody UserProfileUpdateRequestDto request,
                                          @AuthenticationPrincipal User currentUser) {
        return userService.updateProfile(currentUser.getId(), request);
    }

    // -----------------------------------------------------------------------
    // Интересы
    // -----------------------------------------------------------------------

    @Operation(summary = "Список интересов (категорий) текущего пользователя")
    @GetMapping("/me/interests")
    public List<CategoryDto> myInterests(@AuthenticationPrincipal User currentUser) {
        return userService.listInterests(currentUser.getId());
    }

    @Operation(summary = "Добавить категорию в интересы (идемпотентно)")
    @PostMapping("/me/interests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addInterest(@Valid @RequestBody InterestRequestDto request,
                            @AuthenticationPrincipal User currentUser) {
        userService.addInterest(currentUser.getId(), request.categoryId());
    }

    @Operation(summary = "Удалить категорию из интересов")
    @DeleteMapping("/me/interests/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeInterest(@PathVariable Long categoryId,
                               @AuthenticationPrincipal User currentUser) {
        userService.removeInterest(currentUser.getId(), categoryId);
    }

    // -----------------------------------------------------------------------
    // Геопозиция
    // -----------------------------------------------------------------------

    @Operation(summary = "Обновить текущую геопозицию пользователя")
    @PostMapping("/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@Valid @RequestBody LocationUpdateRequestDto request,
                               @AuthenticationPrincipal User currentUser) {
        userService.updateLastKnownLocation(currentUser.getId(), request.lat(), request.lng());
    }
}
