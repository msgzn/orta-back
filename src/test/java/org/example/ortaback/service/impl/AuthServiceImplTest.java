package org.example.ortaback.service.impl;

import org.example.ortaback.config.JwtProperties;
import org.example.ortaback.dto.request.LoginRequestDto;
import org.example.ortaback.dto.request.RegisterRequestDto;
import org.example.ortaback.dto.response.JwtResponseDto;
import org.example.ortaback.exception.InvalidCredentialsException;
import org.example.ortaback.exception.InvalidTokenException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.repository.UniversityRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock UniversityRepository universityRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    JwtProperties jwtProperties = new JwtProperties("secret", 60_000L, 600_000L, "orta-back");

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                universityRepository,
                passwordEncoder,
                authenticationManager,
                tokenProvider,
                jwtProperties
        );
    }

    private RegisterRequestDto registerDto(String email) {
        return new RegisterRequestDto("Alice", "Smith", email, "password123", "+77001112233", null, null);
    }

    // =======================================================================
    //  register
    // =======================================================================

    @Test
    void register_normalizesEmailToLowercase() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("BCRYPT");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");

        authService.register(registerDto("Alice@KazNU.kz"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@kaznu.kz");
    }

    @Test
    void register_bcryptHashesThePassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("BCRYPT_HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");

        authService.register(registerDto("u@example.com"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getHashedPassword()).isEqualTo("BCRYPT_HASH");
    }

    @Test
    void register_autoLinksUniversityByDomainPattern() {
        University kaznu = University.builder().id(7L).name("KazNU").domainPattern("@kaznu.kz").build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(universityRepository.findByDomainPattern("@kaznu.kz")).thenReturn(Optional.of(kaznu));
        when(passwordEncoder.encode(anyString())).thenReturn("BCRYPT");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");

        authService.register(registerDto("student@kaznu.kz"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUniversity()).isEqualTo(kaznu);
    }

    @Test
    void register_duplicateEmail_throwsResourceAlreadyExists() {
        when(userRepository.existsByEmail("dup@kaznu.kz")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerDto("Dup@KazNU.kz")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_returnsBearerTokenPairWithConfiguredTtl() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("BCRYPT");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any())).thenReturn("the-access");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("the-refresh");

        JwtResponseDto response = authService.register(registerDto("u@example.com"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("the-access");
        assertThat(response.refreshToken()).isEqualTo("the-refresh");
        assertThat(response.expiresInMs()).isEqualTo(60_000L);
    }

    // =======================================================================
    //  login
    // =======================================================================

    @Test
    void login_invalidCreds_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("nope"));

        assertThatThrownBy(() ->
                authService.login(new LoginRequestDto("u@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_validCreds_normalizesEmailBeforeAuth() {
        User user = User.builder().id(1L).email("alice@example.com").build();
        Authentication authn = new UsernamePasswordAuthenticationToken(user, null);
        when(authenticationManager.authenticate(any())).thenReturn(authn);
        when(tokenProvider.generateAccessToken(any())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");

        authService.login(new LoginRequestDto("ALICE@Example.com", "pwd"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("alice@example.com");
    }

    // =======================================================================
    //  refresh
    // =======================================================================

    @Test
    void refresh_invalidSignature_throwsInvalidTokenException() {
        when(tokenProvider.isValid("bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void refresh_rejectsAccessTokenSentToRefreshEndpoint() {
        when(tokenProvider.isValid("at")).thenReturn(true);
        when(tokenProvider.isRefreshToken("at")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("at"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not a refresh token");
    }

    @Test
    void refresh_userGone_throwsInvalidTokenException() {
        when(tokenProvider.isValid("rt")).thenReturn(true);
        when(tokenProvider.isRefreshToken("rt")).thenReturn(true);
        when(tokenProvider.getUsername("rt")).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("rt"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("no longer exists");
    }

    @Test
    void refresh_validRefresh_rotatesBothTokens() {
        User user = User.builder().id(1L).email("u@example.com").build();
        when(tokenProvider.isValid("rt")).thenReturn(true);
        when(tokenProvider.isRefreshToken("rt")).thenReturn(true);
        when(tokenProvider.getUsername("rt")).thenReturn("u@example.com");
        when(userRepository.findByEmail("u@example.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(user)).thenReturn("new-at");
        when(tokenProvider.generateRefreshToken(user)).thenReturn("new-rt");

        JwtResponseDto response = authService.refresh("rt");

        assertThat(response.accessToken()).isEqualTo("new-at");
        assertThat(response.refreshToken()).isEqualTo("new-rt");
        verify(tokenProvider).generateAccessToken(eq(user));
        verify(tokenProvider).generateRefreshToken(eq(user));
    }
}
