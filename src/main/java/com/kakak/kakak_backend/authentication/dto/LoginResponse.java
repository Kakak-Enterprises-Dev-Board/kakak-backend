package com.kakak.kakak_backend.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String role;
    private String username;
    private String email;
    private String accountStatus;
    private String message;
    private long accessTokenExpiresInSeconds;
    private long refreshTokenExpiresInSeconds;
    private String sessionId;
    private UserResponse user;
}
