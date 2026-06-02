package org.example.ortaback.persistence.repository;

import org.example.ortaback.persistence.models.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий подписок соц-графа (ТЗ §4.4). Все запросы — derived; реальный
 * SQL прогоняется в {@code FollowIntegrationTest} поверх настоящего PostgreSQL.
 */
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    /** Идемпотентная отписка; выполняется внутри транзакции сервиса. */
    long deleteByFollower_IdAndFollowee_Id(Long followerId, Long followeeId);

    /** Подписчики пользователя («кто на меня подписан»), новые сверху. */
    @EntityGraph(attributePaths = "follower")
    Page<UserFollow> findByFollowee_IdOrderByIdDesc(Long followeeId, Pageable pageable);

    /** Подписки пользователя («на кого я подписан»), новые сверху. */
    @EntityGraph(attributePaths = "followee")
    Page<UserFollow> findByFollower_IdOrderByIdDesc(Long followerId, Pageable pageable);

    long countByFollowee_Id(Long followeeId);

    long countByFollower_Id(Long followerId);
}
