package org.example.ortaback.service;

import org.example.ortaback.dto.request.LoginRequestDto;
import org.example.ortaback.dto.request.RegisterRequestDto;
import org.example.ortaback.dto.response.JwtResponseDto;

public interface AuthService {

    /**
     * Регистрирует нового пользователя и сразу выдаёт пару access + refresh.
     * Email нормализуется в нижний регистр, пароль BCrypt-хешируется,
     * университет (если не передан явно) определяется по
     * {@code domain_pattern}.
     */
    JwtResponseDto register(RegisterRequestDto request);

    /**
     * Проверяет email/password через {@code AuthenticationManager} и выдаёт
     * пару токенов.
     */
    JwtResponseDto login(LoginRequestDto request);

    /**
     * Обменивает refresh-токен на новую пару (rotation). Сам refresh должен
     * быть валидным по подписи/сроку и иметь {@code type=refresh}.
     */
    JwtResponseDto refresh(String refreshToken);
}
