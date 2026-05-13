package org.example.ortaback.controller;

import org.example.ortaback.dto.geo.LocationUpdateRequestDto;
import org.example.ortaback.dto.request.InterestRequestDto;
import org.example.ortaback.dto.request.UserProfileUpdateRequestDto;
import org.example.ortaback.dto.response.CategoryDto;
import org.example.ortaback.dto.response.UniversityMiniDto;
import org.example.ortaback.dto.response.UserProfileDto;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.service.UserService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends MockMvcTestSupport {

    private final UserService userService = mock(UserService.class);
    private MockMvc mvc;

    private final User currentUser = User.builder()
            .id(1L).email("alice@example.com")
            .firstName("Alice").lastName("Smith")
            .hashedPassword("BCRYPT")
            .role(UserRole.USER)
            .build();

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new UserController(userService));
        authenticate(currentUser);
    }

    // -----------------------------------------------------------------------
    //  /me
    // -----------------------------------------------------------------------

    @Test
    void getMyProfile_returnsServiceDto() throws Exception {
        UserProfileDto profile = new UserProfileDto(
                1L, "Alice", "Smith", null, "alice@example.com", null, null,
                UserRole.USER, 0.0f, 10, "ru",
                new UniversityMiniDto(7L, "KazNU"),
                LocalDateTime.of(2026, 5, 1, 12, 0),
                List.of(new CategoryDto(10L, null, "Football", null)),
                null);
        when(userService.getProfile(1L)).thenReturn(profile);

        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.university.name").value("KazNU"))
                .andExpect(jsonPath("$.interests[0].name").value("Football"));
    }

    @Test
    void updateMyProfile_appliesPatch() throws Exception {
        UserProfileDto updated = new UserProfileDto(
                1L, "Alicia", "Smith", null, "alice@example.com",
                null, null, UserRole.USER, 0.0f, 25, "ru", null, null, List.of(), null);
        when(userService.updateProfile(eq(1L), any())).thenReturn(updated);

        UserProfileUpdateRequestDto patchBody = new UserProfileUpdateRequestDto(
                "Alicia", null, null, null, 25, null, null, null, null, null);

        mvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alicia"))
                .andExpect(jsonPath("$.searchRadiusKm").value(25));
    }

    @Test
    void updateMyProfile_radiusOverLimit_returns400() throws Exception {
        UserProfileUpdateRequestDto bad = new UserProfileUpdateRequestDto(
                null, null, null, null, 999, null, null, null, null, null);

        mvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.searchRadiusKm").exists());
    }

    // -----------------------------------------------------------------------
    //  Интересы
    // -----------------------------------------------------------------------

    @Test
    void listMyInterests_returnsCategoryList() throws Exception {
        when(userService.listInterests(1L)).thenReturn(List.of(
                new CategoryDto(10L, null, "Football", null),
                new CategoryDto(11L, null, "Basketball", null)));

        mvc.perform(get("/api/v1/users/me/interests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Football"));
    }

    @Test
    void addInterest_returns204AndCallsService() throws Exception {
        InterestRequestDto body = new InterestRequestDto(10L);

        mvc.perform(post("/api/v1/users/me/interests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(userService).addInterest(1L, 10L);
    }

    @Test
    void removeInterest_returns204() throws Exception {
        mvc.perform(delete("/api/v1/users/me/interests/10"))
                .andExpect(status().isNoContent());

        verify(userService).removeInterest(1L, 10L);
    }

    // -----------------------------------------------------------------------
    //  Геопозиция
    // -----------------------------------------------------------------------

    @Test
    void updateLocation_valid_returns204() throws Exception {
        LocationUpdateRequestDto body = new LocationUpdateRequestDto(43.22, 76.85);

        mvc.perform(post("/api/v1/users/me/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(userService).updateLastKnownLocation(eq(1L), eq(43.22), eq(76.85));
    }

    @Test
    void updateLocation_invalidLat_returns400() throws Exception {
        LocationUpdateRequestDto bad = new LocationUpdateRequestDto(200.0, 0.0);

        mvc.perform(post("/api/v1/users/me/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.lat").exists());
    }

    // -----------------------------------------------------------------------
    //  Аватар
    // -----------------------------------------------------------------------

    @Test
    void uploadAvatar_returnsPublicUrl() throws Exception {
        when(userService.uploadAvatar(eq(1L), any()))
                .thenReturn("https://s3/avatars/new.jpg");

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/v1/users/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://s3/avatars/new.jpg"));
    }

    @Test
    void deleteAvatar_returns204() throws Exception {
        mvc.perform(delete("/api/v1/users/me/avatar"))
                .andExpect(status().isNoContent());

        verify(userService).deleteAvatar(1L);
    }
}
