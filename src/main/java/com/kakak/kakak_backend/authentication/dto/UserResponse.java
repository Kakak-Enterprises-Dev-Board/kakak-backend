package com.kakak.kakak_backend.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String status;
    private Boolean phoneVerified;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
}
