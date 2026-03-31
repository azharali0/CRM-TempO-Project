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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.SALES_REP;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .failedLoginAttempts(0)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        AuthResponse authResponse = buildAuthResponse(user);
        return ApiResponse.success("User registered successfully", authResponse);
    }

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request, String ipAddress) {
        log.info("Login attempt for email: {} from IP: {}", request.getEmail(), ipAddress);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found for email: {} from IP: {}",
                            request.getEmail(), ipAddress);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (user.isAccountLocked()) {
            log.warn("Login attempt on locked account: {} from IP: {}", user.getEmail(), ipAddress);
            throw new UnauthorizedException("Account is locked. Please try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ipAddress);
            throw new UnauthorizedException("Invalid email or password");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        log.info("Login successful for email: {} from IP: {}", user.getEmail(), ipAddress);

        AuthResponse authResponse = buildAuthResponse(user);
        return ApiResponse.success("Login successful", authResponse);
    }

    @Transactional
    public ApiResponse<AuthResponse> refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ApiResponse.success("Token refreshed successfully", authResponse);
    }

    @Transactional
    public ApiResponse<Void> logout(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("User logged out, refresh token revoked");
        return ApiResponse.success("Logged out successfully");
    }

    private void handleFailedLogin(User user, String ipAddress) {
        int newFailedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newFailedAttempts);

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user: {} after {} failed attempts from IP: {}",
                    user.getEmail(), newFailedAttempts, ipAddress);
        } else {
            log.warn("Failed login attempt {} of {} for user: {} from IP: {}",
                    newFailedAttempts, MAX_FAILED_ATTEMPTS, user.getEmail(), ipAddress);
        }

        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user);

        // Save refresh token to DB
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusNanos(
                        jwtTokenProvider.getRefreshTokenExpiration() * 1_000_000L))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
