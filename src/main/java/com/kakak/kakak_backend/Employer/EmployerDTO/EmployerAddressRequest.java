package com.kakak.kakak_backend.Employer.EmployerDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployerAddressRequest {
    @NotBlank
    private String addressLine1;
    @NotBlank
    private String addressLine2;
    @NotBlank
    private String city;
    @NotBlank
    private String state;
    @NotBlank
    private String country;
    @NotBlank
    private String pincode;
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
