package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.dto.response.FollowStatusDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.exception.BadRequestException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.mapper.UserMiniMapper;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.UserFollow;
import org.example.ortaback.persistence.repository.UserFollowRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация соц-графа подписок (ТЗ §4.4). Минимальная follow-модель v2:
 * однонаправленные рёбра, идемпотентные подписка/отписка, дубли отсекаются
 * уникальным ключом {@code (follower_id, followee_id)}.
 *
 * <p>Списки подписчиков/подписок тянут связанного пользователя через
 * {@code @EntityGraph}, поэтому маппинг в {@link UserMiniDto} не триггерит N+1.
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final UserMiniMapper userMiniMapper;

    @Override
    @Transactional
    public void follow(Long targetId, User actor) {
        requireAuth(actor);
        if (actor.getId().equals(targetId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        User target = loadUser(targetId);

        // Идемпотентно: повторная подписка — no-op (и не натыкается на уникальный ключ).
        if (followRepository.existsByFollower_IdAndFollowee_Id(actor.getId(), targetId)) {
            return;
        }
        followRepository.save(UserFollow.builder()
                .follower(actor)
                .followee(target)
                .build());
    }

    @Override
    @Transactional
    public void unfollow(Long targetId, User actor) {
        requireAuth(actor);
        // Идемпотентно: удаление несуществующего ребра — no-op (0 строк).
        followRepository.deleteByFollower_IdAndFollowee_Id(actor.getId(), targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<UserMiniDto> listFollowers(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserFollow> followers = followRepository.findByFollowee_IdOrderByIdDesc(userId, pageable);
        return PageDto.of(followers.map(f -> userMiniMapper.toMini(f.getFollower())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<UserMiniDto> listFollowing(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<UserFollow> following = followRepository.findByFollower_IdOrderByIdDesc(userId, pageable);
        return PageDto.of(following.map(f -> userMiniMapper.toMini(f.getFollowee())));
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusDto getFollowStatus(Long targetId, User viewer) {
        long followersCount = followRepository.countByFollowee_Id(targetId);
        long followingCount = followRepository.countByFollower_Id(targetId);

        boolean followedByMe = false;
        boolean followsMe = false;
        if (viewer != null && !viewer.getId().equals(targetId)) {
            followedByMe = followRepository.existsByFollower_IdAndFollowee_Id(viewer.getId(), targetId);
            followsMe = followRepository.existsByFollower_IdAndFollowee_Id(targetId, viewer.getId());
        }
        return new FollowStatusDto(followedByMe, followsMe, followersCount, followingCount);
    }

    // -----------------------------------------------------------------------
    //  helpers
    // -----------------------------------------------------------------------

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + id));
    }

    private void requireAuth(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    private int clampSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
