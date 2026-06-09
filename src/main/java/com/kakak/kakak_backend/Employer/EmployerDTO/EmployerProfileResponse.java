package com.kakak.kakak_backend.Employer.EmployerDTO;

import com.kakak.kakak_backend.Employer.EmployerEnum.DocumentType;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class EmployerProfileResponse {
    private UUID id;
    private UUID userId;
    private String companyName;
    private String registrationNumber;
    private String companySize;
    private String companyDescription;
    private String industry;
    private String companyWebsite;
    private VerificationStatus verificationStatus;
    private Timestamp createdAt;
    private EmployerAddress address;
    private EmployerDocument document;
    @Data
    @AllArgsConstructor
    public static class EmployerAddress {
        private UUID id;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String country;
        private String pincode;
        private Double latitude;
        private Double longitude;
    }
    @Data
    @AllArgsConstructor
    public static class EmployerDocument {
        private UUID id;
        private UUID fileId;
        private DocumentType documentType;
        private VerificationStatus verificationStatus;
        private Timestamp uploadedAt;
    }
}
