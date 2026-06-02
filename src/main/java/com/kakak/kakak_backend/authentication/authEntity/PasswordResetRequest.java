package com.kakak.kakak_backend.authentication.authEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "PASSWORD_RESET_REQUESTS")
public class PasswordResetRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_hash", length = 128)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private Timestamp otpExpiresAt;

    @Column(name = "otp_verified_at")
    private Timestamp otpVerifiedAt;

    @Column(name = "reset_token_hash", length = 128)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Timestamp resetTokenExpiresAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

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
