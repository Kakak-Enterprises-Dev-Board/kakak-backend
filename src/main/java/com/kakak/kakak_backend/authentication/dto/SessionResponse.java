package com.kakak.kakak_backend.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SessionResponse {
    private UUID id;
    private String deviceName;
    private String deviceOs;
    private String ipAddress;
    private String userAgent;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;
}
