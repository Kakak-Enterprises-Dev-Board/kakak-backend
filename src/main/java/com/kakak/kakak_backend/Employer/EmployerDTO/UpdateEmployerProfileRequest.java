package com.kakak.kakak_backend.Employer.EmployerDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmployerProfileRequest {
    @NotBlank
    private String companyName;
    @NotBlank
    private String registrationNumber;
    @NotBlank
    private String companySize;
    @NotBlank
    private String companyDescription;
    @NotBlank
    private String industry;
    @NotBlank
    private String companyWebsite;
}
