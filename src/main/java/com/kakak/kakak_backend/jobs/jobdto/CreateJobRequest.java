package com.kakak.kakak_backend.jobs.jobdto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "City/Location is required")
    @Size(max = 255, message = "City name must not exceed 255 characters")
    private String city;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Pattern(regexp = "^(FULL_TIME|PART_TIME|CONTRACT|TEMPORARY)$", 
             message = "Employment type must be FULL_TIME, PART_TIME, CONTRACT, or TEMPORARY")
    private String employmentType;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Salary must be non-negative")
    private BigDecimal salary;

    @Size(max = 50, message = "Salary type must not exceed 50 characters")
    private String salaryType;

    private List<String> requiredSkills;

    @Min(value = 1, message = "Vacancies must be at least 1")
    private Integer vacancies;

    private Instant startDate;

    private Instant endDate;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Min(value = 0, message = "Radius must be non-negative")
    private Integer radiusMeters;

    @Size(max = 50, message = "Gender preference must not exceed 50 characters")
    private String genderPreference;
}
