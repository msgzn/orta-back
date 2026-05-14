package org.example.ortaback.controller;

import org.example.ortaback.dto.request.LoginRequestDto;
import org.example.ortaback.dto.request.RefreshTokenRequestDto;
import org.example.ortaback.dto.request.RegisterRequestDto;
import org.example.ortaback.dto.response.JwtResponseDto;
import org.example.ortaback.exception.InvalidCredentialsException;
import org.example.ortaback.exception.InvalidTokenException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.service.AuthService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends MockMvcTestSupport {

    private final AuthService authService = mock(AuthService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new AuthController(authService));
    }

    private JwtResponseDto stubTokenPair() {
        return JwtResponseDto.bearer("access-token", "refresh-token", 86_400_000L);
    }

    // -----------------------------------------------------------------------
    //  register
    // -----------------------------------------------------------------------

    @Test
    void register_validBody_returns201WithTokenPair() throws Exception {
        when(authService.register(any())).thenReturn(stubTokenPair());

        RegisterRequestDto body = new RegisterRequestDto(
                "Alice", "Smith", "alice@kaznu.kz", "password123", "+77001112233", null, null);

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequestDto bad = new RegisterRequestDto(
                "Alice", "Smith", "not-an-email", "password123", null, null, null);

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequestDto bad = new RegisterRequestDto(
                "Alice", "Smith", "alice@kaznu.kz", "short", null, null, null);

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any()))
                .thenThrow(new ResourceAlreadyExistsException("Email already registered"));

        RegisterRequestDto body = new RegisterRequestDto(
                "Alice", "Smith", "alice@kaznu.kz", "password123", null, null, null);

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // -----------------------------------------------------------------------
    //  login
    // -----------------------------------------------------------------------

    @Test
    void login_validCreds_returnsTokenPair() throws Exception {
        when(authService.login(any())).thenReturn(stubTokenPair());

        LoginRequestDto body = new LoginRequestDto("alice@kaznu.kz", "password123");

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_wrongCreds_returns401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        LoginRequestDto body = new LoginRequestDto("alice@kaznu.kz", "wrong");

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // -----------------------------------------------------------------------
    //  refresh
    // -----------------------------------------------------------------------

    @Test
    void refresh_validToken_returnsNewPair() throws Exception {
        when(authService.refresh("the-refresh"))
                .thenReturn(JwtResponseDto.bearer("new-at", "new-rt", 86_400_000L));

        RefreshTokenRequestDto body = new RefreshTokenRequestDto("the-refresh");

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-at"))
                .andExpect(jsonPath("$.refreshToken").value("new-rt"));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(authService.refresh("bad"))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto("bad"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(""))))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    //  logout
    // -----------------------------------------------------------------------

    @Test
    void logout_validBody_returns204() throws Exception {
        org.example.ortaback.dto.request.LogoutRequestDto body =
                new org.example.ortaback.dto.request.LogoutRequestDto("the-refresh");

        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(authService).logout("the-refresh");
    }

    @Test
    void logout_blankToken_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new org.example.ortaback.dto.request.LogoutRequestDto(""))))
                .andExpect(status().isBadRequest());
    }
}
