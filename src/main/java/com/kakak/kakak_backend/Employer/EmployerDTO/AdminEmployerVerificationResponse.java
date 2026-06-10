package com.kakak.kakak_backend.Employer.EmployerDTO;

import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AdminEmployerVerificationResponse {
    private UUID id;
    private String companyName;
    private VerificationStatus verificationStatus;
    private Timestamp createdAt;
}
