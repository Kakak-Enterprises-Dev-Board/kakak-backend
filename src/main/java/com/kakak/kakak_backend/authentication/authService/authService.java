package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.authentication.authEntity.AccountStatus;
import com.kakak.kakak_backend.authentication.authEntity.AuthSession;
import com.kakak.kakak_backend.authentication.authEntity.User;
import com.kakak.kakak_backend.authentication.authRepository.authSessionRepo;
import com.kakak.kakak_backend.authentication.authRepository.userRepo;
import com.kakak.kakak_backend.authentication.dto.LoginRequest;
import com.kakak.kakak_backend.authentication.dto.LoginResponse;
import com.kakak.kakak_backend.authentication.dto.RefreshTokenRequest;
import com.kakak.kakak_backend.authentication.dto.RefreshTokenResponse;
import com.kakak.kakak_backend.authentication.dto.SessionResponse;
import com.kakak.kakak_backend.authentication.dto.UserResponse;
import com.kakak.kakak_backend.authentication.exception.AccountStatusException;
import com.kakak.kakak_backend.authentication.exception.InvalidCredentialsException;
import com.kakak.kakak_backend.authentication.exception.SessionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class authService {
    private final userRepo userRepository;
    private final authSessionRepo sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String loginValue = normalizeLoginValue(request.getUsername());
        User user = findUserByLoginValue(loginValue)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword_hash() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword_hash())) {
            throw new InvalidCredentialsException();
        }

        AccountStatus accountStatus = AccountStatus.from(user.getStatus());
        if (accountStatus != AccountStatus.ACTIVE) {
            throw new AccountStatusException(
                    accountStatus.name(),
                    accountStatus == AccountStatus.NOT_ACTIVATED
                            ? "Account is not activated. Please complete the verification flow."
                            : accountStatus == AccountStatus.IN_PROGRESS
                            ? "Account review is in progress. Please wait for approval."
                            : "Account is not active."
            );
        }

        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        AuthSession session = createSession(user, rememberMe, servletRequest);
        String accessToken = jwtUtil.generateAccessToken(user, session.getId().toString(), rememberMe);
        String refreshToken = jwtUtil.generateRefreshToken(user, session.getId().toString(), rememberMe);
        session.setRefreshTokenHash(hashToken(refreshToken));
        sessionRepository.save(session);

        user.setLast_login_at(Timestamp.from(Instant.now()));
        userRepository.save(user);

        UserResponse userResponse = toUserResponse(user);
        return new LoginResponse(
                true,
                accessToken,
                refreshToken,
                "Bearer",
                jwtUtil.getAccessTokenExpirationSeconds(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getUsername(),
                user.getEmail(),
                accountStatus.name(),
                "Login successful",
                jwtUtil.getAccessTokenExpirationSeconds(),
                jwtUtil.getRefreshTokenExpirationSeconds(rememberMe),
                session.getId().toString(),
                userResponse
        );
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = normalizeToken(request.getRefreshToken());
        if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            throw new InvalidCredentialsException();
        }

        String sessionId = jwtUtil.extractSessionId(refreshToken);
        UUID sessionUuid = parseSessionUuid(sessionId);
        if (sessionUuid == null) {
            throw new InvalidCredentialsException();
        }

        AuthSession session = sessionRepository.findByIdAndRevokedFalse(sessionUuid)
                .orElseThrow(SessionNotFoundException::new);

        if (!hashToken(refreshToken).equals(session.getRefreshTokenHash())) {
            throw new InvalidCredentialsException();
        }

        if (session.getExpiresAt() == null || session.getExpiresAt().toInstant().isBefore(Instant.now())) {
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new SessionNotFoundException();
        }

        session.setUpdatedAt(Timestamp.from(Instant.now()));
        sessionRepository.save(session);
        String accessToken = jwtUtil.generateAccessToken(session.getUser(), session.getId().toString(), session.isRememberMe());
        return new RefreshTokenResponse(accessToken, jwtUtil.getAccessTokenExpirationSeconds(), "Bearer");
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || !jwtUtil.isAccessTokenValid(token)) {
            throw new InvalidCredentialsException();
        }

        String sessionId = jwtUtil.extractSessionId(token);
        UUID sessionUuid = parseSessionUuid(sessionId);
        if (sessionUuid == null) {
            throw new SessionNotFoundException();
        }

        AuthSession session = sessionRepository.findByIdAndRevokedFalse(sessionUuid)
                .orElseThrow(SessionNotFoundException::new);
        session.setRevoked(true);
        sessionRepository.save(session);
    }

    public void logoutAll(String username) {
        User user = findUserByLoginValue(normalizeLoginValue(username))
                .orElseThrow(InvalidCredentialsException::new);
        List<AuthSession> activeSessions = sessionRepository.findAllByUserAndRevokedFalseOrderByCreatedAtDesc(user);
        activeSessions.forEach(session -> session.setRevoked(true));
        sessionRepository.saveAll(activeSessions);
    }

    public List<SessionResponse> getSessions(String username) {
        User user = findUserByLoginValue(normalizeLoginValue(username))
                .orElseThrow(InvalidCredentialsException::new);
        return sessionRepository.findAllByUserAndRevokedFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getCurrentUser(String username) {
        User user = findUserByLoginValue(normalizeLoginValue(username))
                .orElseThrow(InvalidCredentialsException::new);
        return toUserResponse(user);
    }

    private AuthSession createSession(User user, boolean rememberMe, HttpServletRequest servletRequest) {
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setRememberMe(rememberMe);
        session.setDeviceName(resolveDeviceName(servletRequest));
        session.setDeviceOs(resolveDeviceOs(servletRequest));
        session.setIpAddress(resolveClientIp(servletRequest));
        session.setUserAgent(truncate(resolveUserAgent(servletRequest), 512));
        long expiresInSeconds = jwtUtil.getRefreshTokenExpirationSeconds(rememberMe);
        session.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(expiresInSeconds)));
        session.setRevoked(false);
        return session;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() == null ? null : user.getRole().getName(),
                normalizeStatus(user.getStatus()),
                user.getPhone_verified(),
                user.getEmail_verified(),
                user.getLast_login_at() == null ? null : user.getLast_login_at().toInstant(),
                user.getCreated_at() == null ? null : user.getCreated_at().toInstant()
        );
    }

    private SessionResponse toSessionResponse(AuthSession session) {
        return new SessionResponse(
                session.getId(),
                session.getDeviceName(),
                session.getDeviceOs(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getExpiresAt() == null ? null : session.getExpiresAt().toInstant(),
                session.isRevoked(),
                session.getCreatedAt() == null ? null : session.getCreatedAt().toInstant()
        );
    }

    private String normalizeToken(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return value.trim();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7).trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String resolveDeviceName(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String device = request.getHeader("X-Device-Name");
        if (device != null && !device.isBlank()) {
            return device.trim();
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? null : truncate(userAgent, 120);
    }

    private String resolveDeviceOs(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String deviceOs = request.getHeader("X-Device-OS");
        if (deviceOs != null && !deviceOs.isBlank()) {
            return deviceOs.trim();
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return null;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        if (lower.contains("android")) {
            return "Android";
        }
        if (lower.contains("iphone") || lower.contains("ios")) {
            return "iOS";
        }
        if (lower.contains("windows")) {
            return "Windows";
        }
        if (lower.contains("mac os") || lower.contains("macintosh")) {
            return "macOS";
        }
        if (lower.contains("linux")) {
            return "Linux";
        }
        return truncate(userAgent, 60);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String hashToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }

    private UUID parseSessionUuid(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Optional<User> findUserByLoginValue(String loginValue) {
        if (isEmail(loginValue)) {
            return userRepository.findByEmailIgnoreCase(loginValue);
        }
        return userRepository.findByUsernameIgnoreCase(loginValue);
    }

    private String normalizeLoginValue(String loginValue) {
        if (loginValue == null) {
            throw new InvalidCredentialsException();
        }
        String trimmed = loginValue.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return isEmail(trimmed) ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
    }

    private boolean isEmail(String value) {
        return value.contains("@");
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
