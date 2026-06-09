package com.kakak.kakak_backend.jobs.jobdto;

import java.math.BigDecimal;
import java.time.Instant;

public record JobSearchRequest(
        String keyword,
        String city,
        String category,
        String employmentType,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        Instant shiftStartAfter,
        Instant shiftEndBefore,
        String status,
        String userEmail,
        int page,
        int size,
        String sortBy,
        String direction
) {}
