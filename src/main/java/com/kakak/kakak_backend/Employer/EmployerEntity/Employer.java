package com.kakak.kakak_backend.Employer.EmployerEntity;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;
@Entity
@Table(name = "employer")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private AuthUsers user_id;

    @Column(nullable = false)
    private String Company_name;

    @Column(nullable = false)
    private String registration_number;

    @Column(nullable = false)
    private String company_size;

    @Column(nullable = false)
    private String company_description;

    @Column(nullable = false)
    private String industry;

    @Column(nullable = false)
    private String company_website;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp created_at;



}
