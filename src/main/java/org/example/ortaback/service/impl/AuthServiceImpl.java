package org.example.ortaback.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ortaback.config.JwtProperties;
import org.example.ortaback.dto.request.LoginRequestDto;
import org.example.ortaback.dto.request.RegisterRequestDto;
import org.example.ortaback.dto.response.JwtResponseDto;
import org.example.ortaback.exception.InvalidCredentialsException;
import org.example.ortaback.exception.InvalidTokenException;
import org.example.ortaback.exception.ResourceAlreadyExistsException;
import org.example.ortaback.exception.ResourceNotFoundException;
import org.example.ortaback.persistence.models.University;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.persistence.repository.UniversityRepository;
import org.example.ortaback.persistence.repository.UserRepository;
import org.example.ortaback.security.JwtTokenProvider;
import org.example.ortaback.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public JwtResponseDto register(RegisterRequestDto request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("Email already registered: " + email);
        }
        if (request.phoneNumber() != null
                && !request.phoneNumber().isBlank()
                && userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Phone already registered: " + request.phoneNumber());
        }
        if (request.nickname() != null
                && !request.nickname().isBlank()
                && userRepository.existsByNickname(request.nickname())) {
            throw new ResourceAlreadyExistsException(
                    "Nickname already taken: " + request.nickname());
        }

        University university;
        if (request.universityId() != null) {
            university = universityRepository.findById(request.universityId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "University not found: id=" + request.universityId()));
        } else {
            university = detectUniversityFromEmail(email).orElse(null);
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .nickname(emptyToNull(request.nickname()))
                .email(email)
                .hashedPassword(passwordEncoder.encode(request.password()))
                .phoneNumber(emptyToNull(request.phoneNumber()))
                .role(UserRole.USER)
                .trustScore(0.0f)
                .searchRadiusKm(10)
                .university(university)
                .build();

        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto login(LoginRequestDto request) {
        String email = normalizeEmail(request.email());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
            User user = (User) authentication.getPrincipal();
            return issueTokens(user);
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto refresh(String refreshToken) {
        if (!tokenProvider.isValid(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        if (!tokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Provided token is not a refresh token");
        }

        String email = tokenProvider.getUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(
                        "User behind refresh token no longer exists"));

        return issueTokens(user);
    }

    // -----------------------------------------------------------------------

    private JwtResponseDto issueTokens(User user) {
        String access  = tokenProvider.generateAccessToken(user);
        String refresh = tokenProvider.generateRefreshToken(user);
        return JwtResponseDto.bearer(access, refresh, jwtProperties.accessTokenExpiration());
    }

    /**
     * Пытается найти ВУЗ по совпадению {@code domain_pattern} с суффиксом
     * email'а ({@code @kaznu.kz}).
     */
    private Optional<University> detectUniversityFromEmail(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return Optional.empty();
        }
        String suffix = email.substring(at); // включает '@'
        return universityRepository.findByDomainPattern(suffix);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
