package com.kakak.kakak_backend.authentication.authController;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authService.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private UsersService userservice;

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody AuthUsers register ) {
            return ResponseEntity.ok(userservice.registeruser(register));
    }

}
