package com.kakak.kakak_backend.authentication.authService;
import com.kakak.kakak_backend.config.JwtUtil;
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
        private static final int REFRESH_LIMIT = 10; // 10 refreshes per minute per user
        private static final int REGISTER_LIMIT = 5; // 5 registrations per minute per email
        private static final int VERIFY_OTP_LIMIT = 5; // 5 OTP verifications per minute per phone
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        private static final String UNKNOWN = "Unknown";
        private static final String ACCESS_TOKEN = "accessToken";
        private static final String MESSAGE = "message";

        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final UsersRepo usersrepo;
        private final RoleRepo roleRepo;
        private final OtpLogsRepo otpLogsRepo;
        private final OtpRedisService otpRedisService;
        private final RateLimitService rateLimitService;

        private final SessionRepo sessionRepo;
        private final TrustedDeviceRepo trustedDeviceRepo;
        public Map<String, String> registeruser(AuthUsers user) {
            if (user.getUsername().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
            }

            // Apply per-user rate limit for registration
            String email = user.getEmail();
            if (!rateLimitService.isEmailAllowed(email, "register", REGISTER_LIMIT, 1)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Registration rate limit exceeded for this email. Please try again later.");
            }

            if (usersrepo.findByEmail(user.getEmail()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email already registered");
            }

            return registerNewUser(user);
            }


        private Map<String, String> registerNewUser(AuthUsers user) {
            AuthRole role = null;
            if (user.getRole_id() != null && user.getRole_id().getName() != null) {
                String roleName = user.getRole_id().getName().toUpperCase().trim();
                role = roleRepo.findByName(roleName).orElse(null);
            }
            if (role == null) {
                role = roleRepo.findByName("WORKER")
                        .orElseThrow(() -> new RuntimeException("Role not found"));
            }
            user.setRole_id(role);
            if (user.getStatus() == null || user.getStatus().isBlank()) {
                user.setStatus("ACTIVE");
            }
            user.setPassword_hash(passwordEncoder.encode(user.getPassword_hash()));
            user.setLast_login_at(new Timestamp(System.currentTimeMillis()));
            usersrepo.save(user);

            String refreshToken =
                    jwtUtil.GenerateRefreshToken(
                            user.getEmail());

            AuthSession session =
                    new AuthSession();

            session.setUser(user);
            session.setRefresh_token(refreshToken);

            session.setDevice_name(UNKNOWN);
            session.setDevice_os(UNKNOWN);

            session.setIp_address(UNKNOWN);
            session.setUser_agent(UNKNOWN);

            session.setExpires_at(
                    new Timestamp(
                            System.currentTimeMillis()
                                    + (7L * 24 * 60 * 60 * 1000)));
            session.setRevoked(false);
            sessionRepo.save(session);
            Map<String, String> response =
                    new HashMap<>();
            response.put(ACCESS_TOKEN, jwtUtil.GenerateToken(user.getEmail()));
            response.put("refreshToken", refreshToken);

            return response;

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

                // Apply per-user rate limit for token refresh
                if (!rateLimitService.isEmailAllowed(email, "refresh", REFRESH_LIMIT, 1)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Token refresh rate limit exceeded. Please try again later.");
                }
                AuthSession session =
                        sessionRepo.findByRefreshToken(refreshToken)
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Session not found"));

                if (session.isRevoked()) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Session revoked");
                }

                usersrepo.findByEmail(email)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

                Map<String, String> response = new HashMap<>();
                response.put(ACCESS_TOKEN, jwtUtil.GenerateToken(email));
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
            
            // Check if OTP already exists in Redis - if yes, return the same OTP
            String existingPlainOtp = otpRedisService.getPlainOtp(phone, purpose);
            
            //String otp;
            //String otpHash;
            
            if (existingPlainOtp != null) {
                // OTP already exists, return the same one (within rate limit window)
                otp = existingPlainOtp;
                otpHash = otpRedisService.getHashedOtp(phone, purpose);
            } else {
                // Generate new OTP (first request or rate limit window expired)
                otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
                otpHash = passwordEncoder.encode(otp);
                
                // Store both plain and hashed OTP in Redis
                otpRedisService.storeOtp(phone, purpose, otp, otpHash);
            }
            
            // Log the OTP in database (create new log entry or update if needed)
            AuthOtp_logs otpLog = new AuthOtp_logs();
            otpLog.setPhone(phone);
            otpLog.setPurpose(purpose);
            otpLog.setOtp_hash(otpHash);
            otpLog.setAttempts(0);
            otpLog.setExpires_at(Timestamp.from(Instant.now().plusSeconds(OTP_EXPIRATION_MINUTES * 60L)));
            otpLog.setVerified(false);
            otpLogsRepo.save(otpLog);

            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE, "OTP generated");
            response.put("otp", otp);
            return response;
        }

        public Map<String, String> verifyOtp(Map<String, String> request) {
            String phone = getRequiredValue(request, "phone");
            String purpose = getRequiredValue(request, "purpose");
            String otp = getRequiredValue(request, "otp");

            // Apply per-user (per-phone) rate limit for OTP verification
            if (!rateLimitService.isPhoneAllowed(phone, "verify-otp", VERIFY_OTP_LIMIT, 1)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP verification rate limit exceeded. Please try again later.");
            }

            // Check Redis first for faster lookup
            String cachedOtpHash = otpRedisService.getHashedOtp(phone, purpose);
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

            //Delete from database
            otpLogsRepo.delete(otpLog);
            
            // Delete from Redis
            otpRedisService.deleteOtp(phone, purpose);
            
            usersrepo.findByPhone(phone).ifPresent(user -> {
                user.setPhone_verified(true);
                usersrepo.save(user);
            });

            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE, "OTP verified");
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
            response.put(ACCESS_TOKEN, jwtUtil.GenerateToken(email));
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
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active");
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

        session.setDevice_name(UNKNOWN); //for testing purposes only
        session.setDevice_os(UNKNOWN);


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

        String fingerprint =
                user.getId() + "-" + ipAddress;

        TrustedDevice device =
                trustedDeviceRepo
                        .findByUserAndDeviceFingerprint(
                                user,
                                fingerprint)
                        .orElseGet(TrustedDevice::new);

        device.setUser(user);
        device.setDeviceFingerprint(fingerprint);
        device.setDevice_name(UNKNOWN);

        device.setLast_used_at(
                new Timestamp(System.currentTimeMillis())
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
    public void logoutAll(String email) {

        AuthUsers user = usersrepo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"));

        List<AuthSession> sessions =
                sessionRepo.findByUser(user);

        sessions.forEach(session ->
                session.setRevoked(true));

        sessionRepo.saveAll(sessions);
    }
    public void logout(String refreshToken) {

        AuthSession session =
                sessionRepo.findByRefreshToken(refreshToken)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Session not found"));

        session.setRevoked(true);

        sessionRepo.save(session);
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
                        device.getDeviceFingerprint(),
                        device.getDevice_name(),
                        device.getLast_used_at(),
                        device.getCreated_at()
                ))
                .toList();
    }

    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {

        usersrepo.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));

        Map<String, String> otpRequest = new HashMap<>();
        otpRequest.put("phone", request.getPhone());
        otpRequest.put("purpose", "RESET_PASSWORD");

        sendOtp(otpRequest); // Generates OTP in Redis/DB without returning it

        Map<String, String> response = new HashMap<>();
        response.put(MESSAGE, "Reset OTP sent successfully");
        return response;
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
