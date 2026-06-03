package com.kakak.kakak_backend.authentication.authService;

import com.kakak.kakak_backend.Config.JwtUtil;
import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import com.kakak.kakak_backend.authentication.authEntity.AuthOtp_logs;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.OtpLogsRepo;
import com.kakak.kakak_backend.authentication.authRepository.RoleRepo;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
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

        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final UsersRepo usersrepo;
        private final RoleRepo roleRepo;
        private final OtpLogsRepo otpLogsRepo;
        private final OtpRedisService otpRedisService;
        private final RateLimitService rateLimitService;

        public Map<String, String> registeruser(AuthUsers user) {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
            }
            
            // Apply per-user rate limit for registration
            String email = user.getEmail();
            if (!rateLimitService.isEmailAllowed(email, "register", REGISTER_LIMIT, 1)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Registration rate limit exceeded for this email. Please try again later.");
            }
            
            return usersrepo.findByEmail(user.getEmail())
                    .map(existingUser -> {
                        // Username cannot be changed - it remains the same as registered
                        if (!existingUser.getUsername().equals(user.getUsername())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be changed. Use your original registered username.");
                        }
                        return tokensForExistingUser(existingUser, user.getPassword_hash());
                    })
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

                // Apply per-user rate limit for token refresh
                if (!rateLimitService.isEmailAllowed(email, "refresh", REFRESH_LIMIT, 1)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Token refresh rate limit exceeded. Please try again later.");
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
            
            // Check if OTP already exists in Redis - if yes, return the same OTP
            String existingPlainOtp = otpRedisService.getPlainOtp(phone, purpose);
            
            String otp;
            String otpHash;
            
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
            response.put("message", "OTP generated");
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

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
            return usersrepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }
}
