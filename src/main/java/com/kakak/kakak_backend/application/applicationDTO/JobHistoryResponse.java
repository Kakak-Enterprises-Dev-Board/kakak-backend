package com.kakak.kakak_backend.application.applicationDTO;

import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
public class JobHistoryResponse {

    private UUID applicationId;

    private UUID jobId;

    private String status;

    private Timestamp completedAt;
}