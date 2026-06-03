package com.kakak.kakak_backend.authentication.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {

    private UUID id;
    private String deviceName;
    private String deviceOs;
    private String ipAddress;
    private String userAgent;
    private Timestamp expiresAt;
    private boolean revoked;
    private Timestamp createdAt;
}