package org.example.ortaback.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Регистрация нового пользователя.
 *
 * <p>{@code universityId} опционален: если не передан, сервер пытается
 * автоматически определить ВУЗ по доменному шаблону email
 * ({@code University.domainPattern}).
 */
public record RegisterRequestDto(

        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @Size(max = 20) String phoneNumber,
        Long universityId
) {}
