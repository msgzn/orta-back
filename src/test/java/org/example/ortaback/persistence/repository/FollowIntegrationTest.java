package org.example.ortaback.persistence.repository;

import org.example.ortaback.dto.response.FollowStatusDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.exception.BadRequestException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.service.FollowService;
import org.example.ortaback.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционный тест соц-графа подписок (ТЗ §4.4) поверх РЕАЛЬНОГО PostgreSQL
 * (Testcontainers — см. {@link AbstractIntegrationTest}). Прогоняет
 * {@link FollowService} вместе с derived-запросами {@code UserFollowRepository}
 * и уникальным ключом {@code (follower_id, followee_id)} против живой БД.
 *
 * <p>Имя класса оканчивается на {@code Test} (не {@code IT}) — иначе surefire
 * молча пропустил бы тест; та же конвенция, что у {@code ChatIntegrationTest}.
 *
 * <p>{@code @Transactional} → откат после каждого метода. Стратегия
 * {@code IDENTITY} вставляет ребро немедленно на {@code save()}, поэтому
 * порядок id и видимость в последующих запросах детерминированы.
 *
 * <p>Тесты, ожидающие исключение из {@code @Transactional}-метода сервиса
 * (self-follow, отсутствующий пользователь), вынесены в отдельные методы и не
 * делают последующих обращений к БД — брошенное исключение помечает общую
 * транзакцию rollback-only.
 */
@Transactional
class FollowIntegrationTest extends AbstractIntegrationTest {

    @Autowired FollowService followService;
    @Autowired UserRepository userRepository;

    private User userA;
    private User userB;
    private User userC;

    private User user(String tag) {
        return User.builder()
                .firstName(tag).lastName("User")
                .email(tag + "-" + System.nanoTime() + "@example.com")
                .hashedPassword("BCRYPT")
                .trustScore(40.0f)
                .build();
    }

    @BeforeEach
    void seed() {
        userA = userRepository.saveAndFlush(user("a"));
        userB = userRepository.saveAndFlush(user("b"));
        userC = userRepository.saveAndFlush(user("c"));
    }

    @Test
    void followCreatesEdge_visibleInFollowersAndFollowing() {
        followService.follow(userB.getId(), userA);

        PageDto<UserMiniDto> followersOfB = followService.listFollowers(userB.getId(), 0, 20);
        assertThat(followersOfB.content()).extracting(UserMiniDto::id)
                .containsExactly(userA.getId());
        assertThat(followersOfB.totalElements()).isEqualTo(1);

        PageDto<UserMiniDto> followingOfA = followService.listFollowing(userA.getId(), 0, 20);
        assertThat(followingOfA.content()).extracting(UserMiniDto::id)
                .containsExactly(userB.getId());
    }

    @Test
    void followers_orderedNewestFirst() {
        followService.follow(userB.getId(), userA);   // older edge
        followService.follow(userB.getId(), userC);   // newer edge

        PageDto<UserMiniDto> followersOfB = followService.listFollowers(userB.getId(), 0, 20);
        assertThat(followersOfB.content()).extracting(UserMiniDto::id)
                .containsExactly(userC.getId(), userA.getId());   // newest first (id desc)
    }

    @Test
    void follow_isIdempotent() {
        followService.follow(userB.getId(), userA);
        followService.follow(userB.getId(), userA);   // duplicate — no-op, no constraint blow-up

        FollowStatusDto status = followService.getFollowStatus(userB.getId(), userA);
        assertThat(status.followedByMe()).isTrue();
        assertThat(status.followersCount()).isEqualTo(1);
    }

    @Test
    void unfollow_removesEdge_andIsIdempotent() {
        followService.follow(userB.getId(), userA);
        assertThat(followService.getFollowStatus(userB.getId(), userA).followedByMe()).isTrue();

        followService.unfollow(userB.getId(), userA);
        followService.unfollow(userB.getId(), userA);   // idempotent — deleting nothing is fine

        FollowStatusDto status = followService.getFollowStatus(userB.getId(), userA);
        assertThat(status.followedByMe()).isFalse();
        assertThat(status.followersCount()).isZero();
    }

    @Test
    void followStatus_reflectsMutuality() {
        followService.follow(userB.getId(), userA);   // a → b
        followService.follow(userA.getId(), userB);   // b → a

        FollowStatusDto status = followService.getFollowStatus(userB.getId(), userA);
        assertThat(status.followedByMe()).isTrue();    // a follows b
        assertThat(status.followsMe()).isTrue();       // b follows a
        assertThat(status.followersCount()).isEqualTo(1);
        assertThat(status.followingCount()).isEqualTo(1);
    }

    @Test
    void selfFollow_throwsBadRequest() {
        assertThatThrownBy(() -> followService.follow(userA.getId(), userA))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void followMissingUser_throwsResourceNotFound() {
        assertThatThrownBy(() -> followService.follow(999_999_999L, userA))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
