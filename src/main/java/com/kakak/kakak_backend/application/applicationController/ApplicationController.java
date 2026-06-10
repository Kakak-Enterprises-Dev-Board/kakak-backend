package com.kakak.kakak_backend.application.applicationController;

import com.kakak.kakak_backend.application.applicationDTO.JobHistoryResponse;
import com.kakak.kakak_backend.application.applicationService.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/user")
    public List<JobHistoryResponse> getUserJobHistory(
            @RequestParam(required = false)
            String status) {

        return applicationService
                .getUserJobHistory(status);
    }
}