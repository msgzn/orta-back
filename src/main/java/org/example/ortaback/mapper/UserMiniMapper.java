package org.example.ortaback.mapper;

import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.persistence.models.User;
import org.springframework.stereotype.Component;

/**
 * {@link User} → {@link UserMiniDto} для списков соц-графа (ТЗ §4.4).
 *
 * <p>Вызывается на пользователях, подтянутых через {@code @EntityGraph}
 * ({@code follower}/{@code followee}), поэтому lazy-load не триггерится.
 */
@Component
public class UserMiniMapper {

    public UserMiniDto toMini(User user) {
        return new UserMiniDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getTrustScore()
        );
    }
}
