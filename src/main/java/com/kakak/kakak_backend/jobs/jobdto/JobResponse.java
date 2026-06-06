package com.kakak.kakak_backend.jobs.jobdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {
    private UUID id;
    private UUID employerId;
    private String title;
    private String description;
    private String city;
    private String category;
    private String employmentType;
    private BigDecimal salary;
    private String salaryType;
    private Integer vacancies;
    private String status;
    private List<String> requiredSkills;
    private Instant shiftStart;
    private Instant shiftEnd;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer radiusMeters;
    private String genderPreference;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
