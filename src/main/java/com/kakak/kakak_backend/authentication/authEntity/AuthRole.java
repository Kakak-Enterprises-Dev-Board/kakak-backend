package com.kakak.kakak_backend.authentication.authEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRole {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID )
    private UUID role_id;

    @Column(nullable = false)
    private String name;

    private String description;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp created_at;

}


