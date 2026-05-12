package org.example.ortaback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.request.LoginRequestDto;
import org.example.ortaback.dto.request.RefreshTokenRequestDto;
import org.example.ortaback.dto.request.RegisterRequestDto;
import org.example.ortaback.dto.response.JwtResponseDto;
import org.example.ortaback.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация, логин, обновление токенов")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Регистрация — возвращает access + refresh")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public JwtResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authService.register(request);
    }

    @Operation(summary = "Логин по email + password")
    @PostMapping("/login")
    public JwtResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    @Operation(summary = "Обмен refresh-токена на новую пару (rotation)")
    @PostMapping("/refresh")
    public JwtResponseDto refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return authService.refresh(request.refreshToken());
    }
}
