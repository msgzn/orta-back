package org.example.ortaback.service;

import org.example.ortaback.dto.response.FollowStatusDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.persistence.models.User;

/**
 * Соц-граф подписок (ТЗ §4.4 «Друзья», минимальная follow-модель v2).
 * Однонаправленные рёбра: {@code actor} подписывается на {@code targetId}.
 */
public interface FollowService {

    /**
     * Подписаться на пользователя. Идемпотентно: повторный вызов — no-op.
     *
     * @throws org.example.ortaback.exception.BadRequestException     попытка подписаться на себя
     * @throws org.example.ortaback.exception.ResourceNotFoundException целевого пользователя нет
     */
    void follow(Long targetId, User actor);

    /** Отписаться. Идемпотентно: если подписки не было — no-op. */
    void unfollow(Long targetId, User actor);

    /** Подписчики пользователя («кто на него подписан»). */
    PageDto<UserMiniDto> listFollowers(Long userId, int page, int size);

    /** Подписки пользователя («на кого он подписан»). */
    PageDto<UserMiniDto> listFollowing(Long userId, int page, int size);

    /** Состояние отношения {@code viewer} → {@code targetId} плюс счётчики. */
    FollowStatusDto getFollowStatus(Long targetId, User viewer);
}
