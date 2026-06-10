package com.kakak.kakak_backend.Employer.EmployerDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployerDashboardResponse {

    private Integer totalJobs;
    private Integer activeJobs;
    private Integer totalApplications;
    private Integer hiredWorkers;
}