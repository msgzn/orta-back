package org.example.ortaback.controller;

import org.example.ortaback.dto.response.FollowStatusDto;
import org.example.ortaback.dto.response.PageDto;
import org.example.ortaback.dto.response.UserMiniDto;
import org.example.ortaback.exception.BadRequestException;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.service.FollowService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FollowControllerTest extends MockMvcTestSupport {

    private final FollowService followService = mock(FollowService.class);
    private MockMvc mvc;

    private final User currentUser = User.builder()
            .id(1L).email("u@example.com")
            .firstName("U").lastName("U")
            .hashedPassword("BCRYPT").role(UserRole.USER)
            .build();

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new FollowController(followService));
        authenticate(currentUser);
    }

    private UserMiniDto mini(long id, String nickname) {
        return new UserMiniDto(id, "First", "Last", nickname, null, 40.0f);
    }

    @Test
    void follow_returns204() throws Exception {
        mvc.perform(post("/api/v1/users/2/follow"))
                .andExpect(status().isNoContent());

        verify(followService).follow(eq(2L), any());
    }

    @Test
    void unfollow_returns204() throws Exception {
        mvc.perform(delete("/api/v1/users/2/follow"))
                .andExpect(status().isNoContent());

        verify(followService).unfollow(eq(2L), any());
    }

    @Test
    void follow_self_returns400() throws Exception {
        doThrow(new BadRequestException("You cannot follow yourself"))
                .when(followService).follow(eq(1L), any());

        mvc.perform(post("/api/v1/users/1/follow"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void followers_returnsPage() throws Exception {
        when(followService.listFollowers(eq(2L), eq(0), eq(20)))
                .thenReturn(new PageDto<>(List.of(mini(5L, "alice")), 0, 20, 1, 1, false, false));

        mvc.perform(get("/api/v1/users/2/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].nickname").value("alice"));
    }

    @Test
    void following_returnsPage() throws Exception {
        when(followService.listFollowing(eq(2L), eq(0), eq(20)))
                .thenReturn(new PageDto<>(List.of(mini(7L, "bob")), 0, 20, 1, 1, false, false));

        mvc.perform(get("/api/v1/users/2/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].nickname").value("bob"));
    }

    @Test
    void followStatus_returnsDto() throws Exception {
        when(followService.getFollowStatus(eq(2L), any()))
                .thenReturn(new FollowStatusDto(true, false, 10, 3));

        mvc.perform(get("/api/v1/users/2/follow-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedByMe").value(true))
                .andExpect(jsonPath("$.followsMe").value(false))
                .andExpect(jsonPath("$.followersCount").value(10))
                .andExpect(jsonPath("$.followingCount").value(3));
    }
}
