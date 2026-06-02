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
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();

        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final UsersRepo usersrepo;
        private final RoleRepo roleRepo;
        private final OtpLogsRepo otpLogsRepo;

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

            AuthOtp_logs otpLog = new AuthOtp_logs();
            otpLog.setPhone(phone);
            otpLog.setPurpose(purpose);
            otpLog.setOtp_hash(passwordEncoder.encode(otp));
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

            AuthOtp_logs otpLog = otpLogsRepo.findLatestUnverifiedOtp(phone, purpose)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP"));

            if (otpLog.getExpires_at().before(Timestamp.from(Instant.now()))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP expired");
            }

            if (otpLog.getAttempts() >= MAX_OTP_ATTEMPTS) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Maximum OTP attempts exceeded");
            }

            otpLog.setAttempts(otpLog.getAttempts() + 1);
            if (!passwordEncoder.matches(otp, otpLog.getOtp_hash())) {
                otpLogsRepo.save(otpLog);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
            }

            otpLog.setVerified(true);
            otpLogsRepo.save(otpLog);
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
