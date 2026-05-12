package org.example.ortaback.service;

import org.example.ortaback.dto.request.UserProfileUpdateRequestDto;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.UserProfileDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    /**
     * Обновляет {@code last_known_location} и {@code location_updated_at}
     * указанного пользователя. Координаты валидируются перед записью.
     */
    void updateLastKnownLocation(Long userId, double lat, double lng);

    /** Полный профиль текущего пользователя со списком интересов. */
    UserProfileDto getProfile(Long userId);

    /** Частичное обновление профиля (PATCH-семантика: {@code null} = не менять). */
    UserProfileDto updateProfile(Long userId, UserProfileUpdateRequestDto request);

    /** Список интересов (категорий) пользователя. */
    List<CategoryDto> listInterests(Long userId);

    /**
     * Добавляет категорию в интересы. Идемпотентен — повторный POST с тем же
     * categoryId не создаёт дубликат (уникальный констрейнт + предварительная
     * проверка).
     */
    void addInterest(Long userId, Long categoryId);

    void removeInterest(Long userId, Long categoryId);

    /**
     * Загружает аватар пользователя в S3, удаляет предыдущий (best-effort)
     * и сохраняет новый URL в {@code User.avatarUrl}.
     *
     * @return публичный URL загруженного аватара
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * Удаляет текущий аватар пользователя из S3 (best-effort) и обнуляет
     * {@code User.avatarUrl}.
     */
    void deleteAvatar(Long userId);
}
