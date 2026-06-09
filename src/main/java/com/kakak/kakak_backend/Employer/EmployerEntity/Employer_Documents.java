package com.kakak.kakak_backend.Employer.EmployerEntity;
import com.kakak.kakak_backend.Employer.EmployerEnum.DocumentType;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import com.kakak.kakak_backend.Files.fileEntity.files;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.UUID;
@Entity
@Data
@Table(name = "employer_documents")
@AllArgsConstructor
@NoArgsConstructor
public class Employer_Documents {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Employer employer_id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private files file_id;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @CreationTimestamp
    @Column(nullable = false)
    private Timestamp uploaded_at;

}
