package com.crm.service;

import com.crm.config.JwtTokenProvider;
import com.crm.dto.request.LoginRequest;
import com.crm.dto.request.RegisterRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.AuthResponse;
import com.crm.exception.ConflictException;
import com.crm.exception.UnauthorizedException;
import com.crm.model.entity.RefreshToken;
import com.crm.model.entity.User;
import com.crm.model.enums.UserRole;
import com.crm.repository.RefreshTokenRepository;
import com.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(UserRole.SALES_REP)
                .enabled(true)
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setRole(UserRole.SALES_REP);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);

        ApiResponse<AuthResponse> response = authService.register(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("test@example.com", response.getData().getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refreshToken");
        when(jwtTokenProvider.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);

        ApiResponse<AuthResponse> response = authService.login(request, "127.0.0.1");

        assertTrue(response.isSuccess());
        assertEquals("accessToken", response.getData().getAccessToken());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(request, "127.0.0.1"));
    }

    @Test
    void login_lockedAccount_throwsUnauthorized() {
        testUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("locked"));
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(request, "127.0.0.1"));
    }

    @Test
    void login_incrementsFailedAttempts() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(request, "127.0.0.1"));
        assertEquals(1, testUser.getFailedLoginAttempts());
    }

    @Test
    void login_locksAccountAfter5FailedAttempts() {
        testUser.setFailedLoginAttempts(4);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertThrows(UnauthorizedException.class,
                () -> authService.login(request, "127.0.0.1"));
        assertNotNull(testUser.getAccountLockedUntil());
        assertEquals(5, testUser.getFailedLoginAttempts());
    }

    @Test
    void refreshToken_valid() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("validToken")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("newAccessToken");

        ApiResponse<AuthResponse> response = authService.refreshToken("validToken");

        assertTrue(response.isSuccess());
        assertEquals("newAccessToken", response.getData().getAccessToken());
    }

    @Test
    void refreshToken_revoked_throwsUnauthorized() {
        RefreshToken refreshToken = RefreshToken.builder()
                .token("revokedToken")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("revokedToken")).thenReturn(Optional.of(refreshToken));

        assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken("revokedToken"));
    }
}
