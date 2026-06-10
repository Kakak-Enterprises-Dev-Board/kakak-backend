package com.kakak.kakak_backend.application.applicationService;

import com.kakak.kakak_backend.application.applicationDTO.JobHistoryResponse;

import java.util.List;

public interface ApplicationService {

    List<JobHistoryResponse> getUserJobHistory(String status);
}