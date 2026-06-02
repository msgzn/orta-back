package org.example.ortaback.service.impl;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты {@link FollowServiceImpl} (ТЗ §4.4) на моках — проверяют
 * идемпотентность подписки/отписки, запрет self-follow, 404 на отсутствующего
 * пользователя, корректную сторону маппинга в списках и вычисление
 * {@code FollowStatusDto}. Реальный SQL derived-методов прогоняется отдельно
 * в {@code FollowIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UserFollowRepository followRepository;
    @Mock UserMiniMapper userMiniMapper;

    @InjectMocks FollowServiceImpl service;

    private final User actor  = User.builder().id(1L).firstName("Actor").build();
    private final User target = User.builder().id(2L).firstName("Target").build();

    private UserMiniDto someMini() {
        return new UserMiniDto(99L, "F", "L", "nick", null, 40.0f);
    }

    // -----------------------------------------------------------------------
    //  follow
    // -----------------------------------------------------------------------

    @Test
    void follow_persistsEdge_whenNotAlreadyFollowing() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.existsByFollower_IdAndFollowee_Id(1L, 2L)).thenReturn(false);

        service.follow(2L, actor);

        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        verify(followRepository).save(captor.capture());
        UserFollow saved = captor.getValue();
        assertThat(saved.getFollower()).isSameAs(actor);
        assertThat(saved.getFollowee()).isSameAs(target);
    }

    @Test
    void follow_self_throwsBadRequest() {
        assertThatThrownBy(() -> service.follow(1L, actor))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(userRepository, followRepository);
    }

    @Test
    void follow_alreadyFollowing_isNoOp() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(followRepository.existsByFollower_IdAndFollowee_Id(1L, 2L)).thenReturn(true);

        service.follow(2L, actor);

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_targetNotFound_throwsResourceNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(2L, actor))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void follow_nullActor_throwsAccessDenied() {
        assertThatThrownBy(() -> service.follow(2L, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(userRepository, followRepository);
    }

    // -----------------------------------------------------------------------
    //  unfollow
    // -----------------------------------------------------------------------

    @Test
    void unfollow_deletesEdge() {
        service.unfollow(2L, actor);

        verify(followRepository).deleteByFollower_IdAndFollowee_Id(1L, 2L);
    }

    @Test
    void unfollow_nullActor_throwsAccessDenied() {
        assertThatThrownBy(() -> service.unfollow(2L, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(followRepository);
    }

    // -----------------------------------------------------------------------
    //  listFollowers / listFollowing
    // -----------------------------------------------------------------------

    @Test
    void listFollowers_mapsFollowerSide() {
        User follower = User.builder().id(5L).firstName("Follower").build();
        UserFollow edge = UserFollow.builder().id(99L).follower(follower).followee(target).build();
        when(followRepository.findByFollowee_IdOrderByIdDesc(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(edge)));
        when(userMiniMapper.toMini(any(User.class))).thenReturn(someMini());

        PageDto<UserMiniDto> page = service.listFollowers(2L, 0, 20);

        assertThat(page.content()).hasSize(1);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMiniMapper).toMini(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);   // follower side
    }

    @Test
    void listFollowing_mapsFolloweeSide() {
        User followee = User.builder().id(7L).firstName("Followee").build();
        UserFollow edge = UserFollow.builder().id(99L).follower(actor).followee(followee).build();
        when(followRepository.findByFollower_IdOrderByIdDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(edge)));
        when(userMiniMapper.toMini(any(User.class))).thenReturn(someMini());

        PageDto<UserMiniDto> page = service.listFollowing(1L, 0, 20);

        assertThat(page.content()).hasSize(1);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMiniMapper).toMini(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(7L);   // followee side
    }

    // -----------------------------------------------------------------------
    //  getFollowStatus
    // -----------------------------------------------------------------------

    @Test
    void getFollowStatus_computesFlagsAndCounts() {
        when(followRepository.countByFollowee_Id(2L)).thenReturn(10L);
        when(followRepository.countByFollower_Id(2L)).thenReturn(3L);
        when(followRepository.existsByFollower_IdAndFollowee_Id(1L, 2L)).thenReturn(true);   // followedByMe
        when(followRepository.existsByFollower_IdAndFollowee_Id(2L, 1L)).thenReturn(false);  // followsMe

        FollowStatusDto status = service.getFollowStatus(2L, actor);

        assertThat(status.followedByMe()).isTrue();
        assertThat(status.followsMe()).isFalse();
        assertThat(status.followersCount()).isEqualTo(10L);
        assertThat(status.followingCount()).isEqualTo(3L);
    }

    @Test
    void getFollowStatus_self_flagsFalseButCountsReturned() {
        when(followRepository.countByFollowee_Id(1L)).thenReturn(4L);
        when(followRepository.countByFollower_Id(1L)).thenReturn(8L);

        FollowStatusDto status = service.getFollowStatus(1L, actor);

        assertThat(status.followedByMe()).isFalse();
        assertThat(status.followsMe()).isFalse();
        assertThat(status.followersCount()).isEqualTo(4L);
        assertThat(status.followingCount()).isEqualTo(8L);
    }
}
