package com.kakak.kakak_backend.authentication.authEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "trusted_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AuthUsers user;

    private String deviceFingerprint;

    private String device_name;

    private Timestamp last_used_at;

    @CreationTimestamp
    private Timestamp created_at;
}