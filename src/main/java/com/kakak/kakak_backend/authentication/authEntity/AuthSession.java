package com.kakak.kakak_backend.authentication.authEntity;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AuthUsers user;

    private String refresh_token;

    private String device_name;

    private String device_os;

    private String ip_address;

    @Column(length = 1000)
    private String user_agent;

    private Timestamp expires_at;

    private boolean revoked;

    @CreationTimestamp
    private Timestamp created_at;
}