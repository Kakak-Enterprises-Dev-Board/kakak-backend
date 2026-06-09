package com.kakak.kakak_backend.authentication.authController;

import com.kakak.kakak_backend.authentication.authDTO.*;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authService.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsersService userservice;

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody RegisterRequest register ) {
        return ResponseEntity.ok(userservice.registeruser(register));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String,String>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request == null ? null : request.get("refreshToken");
        return ResponseEntity.ok(userservice.refreshAccessToken(refreshToken));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(userservice.sendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(userservice.verifyOtp(request));
    }
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        return ResponseEntity.ok(
                userservice.login(request, httpRequest)
        );
    }
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(
            Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                userservice.getCurrentUser(email)
        );
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody Map<String,String> request) {

        userservice.logout(
                request.get("refreshToken"));

        return ResponseEntity.ok().build();
    }
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            Authentication authentication) {

        userservice.logoutAll(
                authentication.getName());

        return ResponseEntity.ok().build();
    }
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(
            Authentication authentication) {

        return ResponseEntity.ok(
                userservice.getSessions(authentication.getName())
        );
    }
    @GetMapping("/trusted-devices")
    public ResponseEntity<List<TrustedDeviceResponse>> trustedDevices(
            Authentication authentication) {

        return ResponseEntity.ok(
                userservice.getTrustedDevices(authentication.getName())
        );
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        return ResponseEntity.ok(userservice.forgotPassword(request));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        userservice.resetPassword(request);

        return ResponseEntity.ok().build();
    }
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        userservice.changePassword(
                authentication.getName(),
                request);

        return ResponseEntity.ok().build();
    }

}
