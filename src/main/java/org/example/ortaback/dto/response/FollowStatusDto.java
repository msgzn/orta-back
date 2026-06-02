package org.example.ortaback.dto.response;

/**
 * Состояние отношения текущего пользователя к целевому профилю (ТЗ §4.4).
 * Питает кнопку «Подписаться/Отписаться» и счётчики на публичном профиле.
 *
 * @param followedByMe  подписан ли текущий пользователь на целевого
 * @param followsMe     подписан ли целевой пользователь на текущего (взаимность)
 * @param followersCount сколько всего подписчиков у целевого пользователя
 * @param followingCount на скольких подписан целевой пользователь
 */
public record FollowStatusDto(
        boolean followedByMe,
        boolean followsMe,
        long followersCount,
        long followingCount
) {
}
