package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.Config.JwtUtil;
import com.kakak.kakak_backend.authentication.authDTO.*;
import com.kakak.kakak_backend.authentication.authEntity.*;
import com.kakak.kakak_backend.authentication.authRepository.*;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class UsersService implements UserDetailsService {
        private static final int OTP_EXPIRATION_MINUTES = 5;
        private static final int MAX_OTP_ATTEMPTS = 5;
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final UsersRepo usersrepo;
        private final RoleRepo roleRepo;
        private final OtpLogsRepo otpLogsRepo;
        private final OtpRedisService otpRedisService;
        private final SessionRepo sessionRepo;
        private final TrustedDeviceRepo trustedDeviceRepo;
        public Map<String, String> registeruser(AuthUsers user) {
            return usersrepo.findByEmail(user.getEmail())
                    .map(existingUser -> tokensForExistingUser(existingUser, user.getPassword_hash()))
                    .orElseGet(() -> registerNewUser(user));
        }

        private Map<String, String> registerNewUser(AuthUsers user) {
            AuthRole role = roleRepo.findByName("WORKER")
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole_id(role);
            if (user.getStatus() == null || user.getStatus().isBlank()) {
                user.setStatus("ACTIVE");
            }
            user.setPassword_hash(passwordEncoder.encode(user.getPassword_hash()));
            usersrepo.save(user);
            return createTokenResponse(user.getEmail());
        }

        private Map<String, String> tokensForExistingUser(AuthUsers existingUser, String rawPassword) {
            if (!passwordEncoder.matches(rawPassword, existingUser.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            return createTokenResponse(existingUser.getEmail());
        }

        public Map<String, String> refreshAccessToken(String refreshToken) {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
            }

            try {
                String email = jwtUtil.extractEmail(refreshToken);
                if (!jwtUtil.isRefreshTokenValid(refreshToken, email)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
                }

                usersrepo.findByEmail(email)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

                Map<String, String> response = new HashMap<>();
                response.put("accessToken", jwtUtil.GenerateToken(email));
                return response;
            } catch (JwtException | IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }
        }

        public Map<String, String> sendOtp(Map<String, String> request) {
            String phone = getRequiredValue(request, "phone");
            String purpose = getRequiredValue(request, "purpose");
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            String otpHash = passwordEncoder.encode(otp);
            
            AuthOtp_logs otpLog = new AuthOtp_logs();
            otpLog.setPhone(phone);
            otpLog.setPurpose(purpose);
            otpLog.setOtp_hash(otpHash);
            otpLog.setAttempts(0);
            otpLog.setExpires_at(Timestamp.from(Instant.now().plusSeconds(OTP_EXPIRATION_MINUTES * 60L)));
            otpLog.setVerified(false);
            otpLogsRepo.save(otpLog);
            
            // Store OTP in Redis with TTL
            otpRedisService.storeOtp(phone, purpose, otpHash);

            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP generated");
            response.put("otp", otp);
            return response;
        }

        public Map<String, String> verifyOtp(Map<String, String> request) {
            String phone = getRequiredValue(request, "phone");
            String purpose = getRequiredValue(request, "purpose");
            String otp = getRequiredValue(request, "otp");

            // Check Redis first for faster lookup
            String cachedOtpHash = otpRedisService.getOtp(phone, purpose);
            AuthOtp_logs otpLog;
            
            if (cachedOtpHash != null) {
                // Found in Redis - verify immediately
                if (!passwordEncoder.matches(otp, cachedOtpHash)) {
                    // Update attempts in database
                    otpLogsRepo.findLatestUnverifiedOtp(phone, purpose).ifPresent(log -> {
                        log.setAttempts(log.getAttempts() + 1);
                        otpLogsRepo.save(log);
                    });
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
                }
                
                // OTP is valid, fetch from database to update status
                otpLog = otpLogsRepo.findLatestUnverifiedOtp(phone, purpose)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP"));
            } else {
                // Not in Redis, fetch from database
                otpLog = otpLogsRepo.findLatestUnverifiedOtp(phone, purpose)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP"));
                
                if (otpLog.getExpires_at().before(Timestamp.from(Instant.now()))) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP expired");
                }
                
                if (!passwordEncoder.matches(otp, otpLog.getOtp_hash())) {
                    otpLog.setAttempts(otpLog.getAttempts() + 1);
                    otpLogsRepo.save(otpLog);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
                }
            }

            if (otpLog.getExpires_at().before(Timestamp.from(Instant.now()))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP expired");
            }

            if (otpLog.getAttempts() >= MAX_OTP_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Maximum OTP attempts exceeded");
            }

            // Mark as verified in database
            otpLog.setVerified(true);
            otpLogsRepo.save(otpLog);
            
            // Delete from Redis
            otpRedisService.deleteOtp(phone, purpose);
            
            usersrepo.findByPhone(phone).ifPresent(user -> {
                user.setPhone_verified(true);
                usersrepo.save(user);
            });

            Map<String, String> response = new HashMap<>();
            response.put("message", "OTP verified");
            response.put("verified", "true");
            return response;
        }

        private String getRequiredValue(Map<String, String> request, String fieldName) {
            if (request == null || request.get(fieldName) == null || request.get(fieldName).isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
            }
            return request.get(fieldName).trim();
        }

        private Map<String, String> createTokenResponse(String email) {
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", jwtUtil.GenerateToken(email));
            response.put("refreshToken", jwtUtil.GenerateRefreshToken(email));
            return response;
        }
    public TokenResponse login(
            LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        AuthUsers user = usersrepo.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid credentials"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid credentials");
        }

        user.setLast_login_at(
                new java.sql.Timestamp(System.currentTimeMillis()));

        usersrepo.save(user);
        String refreshToken =
                jwtUtil.GenerateRefreshToken(user.getEmail());

        String ipAddress =
                httpRequest.getRemoteAddr();

        String userAgent =
                httpRequest.getHeader("User-Agent");

        AuthSession session = new AuthSession();

        session.setUser(user);
        session.setRefresh_token(refreshToken);

        session.setDevice_name("Postman");
        session.setDevice_os("Windows");

        session.setIp_address(ipAddress);
        session.setUser_agent(userAgent);

        session.setExpires_at(
                new java.sql.Timestamp(
                        System.currentTimeMillis()
                                + (7L * 24 * 60 * 60 * 1000)
                )
        );

        session.setRevoked(false);

        sessionRepo.save(session);

        TrustedDevice device =
                new TrustedDevice();

        device.setUser(user);

        device.setDevice_fingerprint(
                user.getId() + "-" + ipAddress
        );

        device.setDevice_name("Postman");

        device.setLast_used_at(
                new java.sql.Timestamp(
                        System.currentTimeMillis()
                )
        );

        trustedDeviceRepo.save(device);

        LoginUserResponse userResponse =
                new LoginUserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getPhone(),
                        user.getEmail(),
                        user.getRole_id().getName(),
                        user.getStatus(),
                        user.isPhone_verified(),
                        user.isEmail_verified(),
                        user.getCreated_at()
                );

        return new TokenResponse(
                jwtUtil.GenerateToken(user.getEmail()),
                refreshToken,
                9000L,
                userResponse
        );
    }
    public UserProfileResponse getCurrentUser(String email) {

        AuthUsers user = usersrepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole_id().getName(),
                user.getStatus(),
                user.isPhone_verified(),
                user.isEmail_verified(),
                user.getCreated_at()
        );
    }
    public List<SessionResponse> getSessions(String email) {

        AuthUsers user = usersrepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return sessionRepo.findByUser(user)
                .stream()
                .map(session -> new SessionResponse(
                        session.getId(),
                        session.getDevice_name(),
                        session.getDevice_os(),
                        session.getIp_address(),
                        session.getUser_agent(),
                        session.getExpires_at(),
                        session.isRevoked(),
                        session.getCreated_at()
                ))
                .toList();
    }
    public List<TrustedDeviceResponse> getTrustedDevices(String email) {

        AuthUsers user = usersrepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        return trustedDeviceRepo.findByUser(user)
                .stream()
                .map(device -> new TrustedDeviceResponse(
                        device.getId(),
                        device.getDevice_fingerprint(),
                        device.getDevice_name(),
                        device.getLast_used_at(),
                        device.getCreated_at()
                ))
                .toList();
    }

    public void forgotPassword(ForgotPasswordRequest request) {

        usersrepo.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        Map<String, String> otpRequest = new HashMap<>();
        otpRequest.put("phone", request.getPhone());
        otpRequest.put("purpose", "RESET_PASSWORD");

        sendOtp(otpRequest);
    }

    public void resetPassword(ResetPasswordRequest request) {

        AuthUsers user = usersrepo.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid password reset request"));

        Map<String, String> otpRequest = new HashMap<>();
        otpRequest.put("phone", request.getPhone());
        otpRequest.put("purpose", "RESET_PASSWORD");
        otpRequest.put("otp", request.getOtp());

        verifyOtp(otpRequest);

        user.setPassword_hash(
                passwordEncoder.encode(request.getNewPassword()));

        usersrepo.save(user);
    }

    public void changePassword(
            String email,
            ChangePasswordRequest request) {

        AuthUsers user = usersrepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid current password");
        }

        user.setPassword_hash(
                passwordEncoder.encode(
                        request.getNewPassword()));

        usersrepo.save(user);
    }
        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            return usersrepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
}
