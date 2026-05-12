package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.geo.LocationUpdateRequestDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Эндпоинты пользователя. На этой итерации — только обновление геопозиции;
 * прочие операции (профиль, интересы) поедут с {@code AuthController}.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Обновить текущую геопозицию пользователя")
    @PostMapping("/me/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@Valid @RequestBody LocationUpdateRequestDto request,
                               @AuthenticationPrincipal User currentUser) {
        userService.updateLastKnownLocation(currentUser.getId(), request.lat(), request.lng());
    }
}
