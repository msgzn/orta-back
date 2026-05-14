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
     *
     * <p>После успешной ротации старый JTI добавляется в blacklist — повторное
     * использование того же refresh-токена возвращает 401, даже если кто-то
     * перехватил его одновременно с легитимным юзером.
     */
    JwtResponseDto refresh(String refreshToken);

    /**
     * Logout: помечает refresh-токен отозванным до его естественного
     * истечения. Идемпотентно — повторный logout с уже отозванным или
     * невалидным токеном просто возвращается без шума.
     */
    void logout(String refreshToken);
}
