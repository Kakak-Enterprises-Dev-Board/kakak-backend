package com.kakak.kakak_backend.authentication.authEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;
import java.util.UUID;

import jakarta.persistence.Column;

public class AuthRate_limit_logs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String ip_address;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private int request_count;

    @Column(nullable = false)
    private Timestamp blocked_until;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp created_at;





}
