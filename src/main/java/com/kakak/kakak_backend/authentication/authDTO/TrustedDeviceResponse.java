package com.kakak.kakak_backend.authentication.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrustedDeviceResponse {

    private UUID id;
    private String deviceFingerprint;
    private String deviceName;
    private Timestamp lastUsedAt;
    private Timestamp createdAt;
}
