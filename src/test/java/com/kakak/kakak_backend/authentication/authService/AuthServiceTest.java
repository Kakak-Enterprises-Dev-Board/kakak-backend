package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.authentication.authEntity.AccountStatus;
import com.kakak.kakak_backend.authentication.authEntity.AuthSession;
import com.kakak.kakak_backend.authentication.authEntity.Role;
import com.kakak.kakak_backend.authentication.authEntity.User;
import com.kakak.kakak_backend.authentication.authRepository.authSessionRepo;
import com.kakak.kakak_backend.authentication.authRepository.userRepo;
import com.kakak.kakak_backend.authentication.dto.LoginRequest;
import com.kakak.kakak_backend.authentication.exception.AccountStatusException;
import com.kakak.kakak_backend.authentication.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private userRepo userRepository;

    @Mock
    private authSessionRepo sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private authService authService;

    @BeforeEach
    void setUp() {
        authService = new authService(userRepository, sessionRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void loginWithEmailCreatesSessionAndTokens() {
        User user = activeAdminUser();
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@example.com");
        request.setPassword("secret");
        request.setRememberMe(true);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("User-Agent", "Mozilla/5.0");

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPassword_hash())).thenReturn(true);
        when(jwtUtil.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenExpirationSeconds(true)).thenReturn(2_592_000L);
        when(jwtUtil.generateAccessToken(any(User.class), any(String.class), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(User.class), any(String.class), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn("refresh-token");
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(request, servletRequest);

        assertTrue(response.isSuccess());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("ADMIN", response.getRole());
        assertEquals("ACTIVE", response.getAccountStatus());
        assertNotNull(response.getSessionId());
        verify(sessionRepository).save(any(AuthSession.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginRejectsInactiveAccount() {
        User user = activeAdminUser();
        user.setStatus(AccountStatus.NOT_ACTIVATED.name());

        LoginRequest request = new LoginRequest();
        request.setUsername("admin@example.com");
        request.setPassword("secret");

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPassword_hash())).thenReturn(true);

        assertThrows(AccountStatusException.class, () -> authService.login(request, new MockHttpServletRequest()));
    }

    @Test
    void loginRejectsInvalidPassword() {
        User user = activeAdminUser();

        LoginRequest request = new LoginRequest();
        request.setUsername("admin@example.com");
        request.setPassword("wrong");

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword_hash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, new MockHttpServletRequest()));
    }

    private User activeAdminUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin-user");
        user.setEmail("admin@example.com");
        user.setPassword_hash("$2a$10$hash");
        user.setStatus(AccountStatus.ACTIVE.name());
        user.setPhone_verified(true);
        user.setEmail_verified(true);
        user.setCreated_at(Timestamp.from(Instant.now()));

        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("ADMIN");
        user.setRole(role);
        return user;
    }
}
