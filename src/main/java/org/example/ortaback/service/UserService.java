package org.example.ortaback.service;

public interface UserService {

    /**
     * Обновляет {@code last_known_location} и {@code location_updated_at}
     * указанного пользователя. Координаты валидируются перед записью.
     */
    void updateLastKnownLocation(Long userId, double lat, double lng);
}
