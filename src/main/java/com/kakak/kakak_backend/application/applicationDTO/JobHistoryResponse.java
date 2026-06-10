package com.kakak.kakak_backend.application.applicationDTO;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class JobHistoryResponse {

    private UUID applicationId;

    private UUID jobId;

    private String status;

    private Instant completedAt;
}