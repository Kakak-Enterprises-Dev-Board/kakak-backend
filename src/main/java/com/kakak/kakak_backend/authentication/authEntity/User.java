package com.kakak.kakak_backend.authentication.authEntity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Entity
@Table(name= "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String phone;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password_hash;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    private String status;
    private Boolean phone_verified;
    private Boolean email_verified;
    private Timestamp last_login_at;
    private Timestamp created_at;
    private Timestamp updated_at;
}
