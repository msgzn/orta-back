package org.example.ortaback.dto.response;

/**
 * Счётчик непрочитанных уведомлений — для бейджа на колокольчике
 * ({@code GET /api/v1/notifications/unread-count}).
 */
public record UnreadCountDto(long count) {}
