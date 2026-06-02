package com.kakak.kakak_backend.authentication.authController;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authService.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsersService userservice;

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody AuthUsers register ) {
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

}
