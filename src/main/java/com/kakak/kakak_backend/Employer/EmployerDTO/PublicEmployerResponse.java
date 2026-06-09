package com.kakak.kakak_backend.Employer.EmployerDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PublicEmployerResponse {
    private UUID id;
    private String companyName;
    private String companyDescription;
    private String industry;
    private String companyWebsite;
    private boolean verified;
    private Timestamp createdAt;
}
