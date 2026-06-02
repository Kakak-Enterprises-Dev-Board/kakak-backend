package com.kakak.kakak_backend.authentication.authEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "SESSIONS")
public class AuthSession {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_os")
    private String deviceOs;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        if (createdAt == null) {
            createdAt = timestamp;
        }
        updatedAt = timestamp;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Timestamp.from(Instant.now());
    }
}
