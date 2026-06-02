package com.kakak.kakak_backend.authentication.authEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "otp_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthOtp_logs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String otp_hash;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Timestamp expires_at;

    @Column(nullable = false)
    private boolean verified;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp created_at;


}
