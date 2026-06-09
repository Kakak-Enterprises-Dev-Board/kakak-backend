package com.kakak.kakak_backend.jobs.jobcontroller;

import com.kakak.kakak_backend.jobs.jobdto.CreateJobRequest;
import com.kakak.kakak_backend.jobs.jobdto.JobResponse;
import com.kakak.kakak_backend.jobs.jobdto.PublishJobRequest;
import com.kakak.kakak_backend.jobs.jobdto.JobSearchRequest;
import com.kakak.kakak_backend.jobs.jobservice.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, authentication.getName()));
    }

    @PostMapping("/drafts")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> saveDraft(
            @RequestBody CreateJobRequest request, // No @Valid as drafts can be incomplete
            Authentication authentication) {
        return ResponseEntity.ok(jobService.saveDraft(request, authentication.getName()));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> publishDraft(
            @Valid @RequestBody PublishJobRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.publishDraft(request.getDraftId(), authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) Instant shiftStartAfter,
            @RequestParam(required = false) Instant shiftEndBefore,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication) {
        return searchJobs(
                keyword, city, category, employmentType, salaryMin, salaryMax,
                shiftStartAfter, shiftEndBefore, status,
                page, size, sortBy, direction,
                authentication
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) Instant shiftStartAfter,
            @RequestParam(required = false) Instant shiftEndBefore,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication) {
        Page<JobResponse> jobPage = jobService.searchAndFilterJobs(
                new JobSearchRequest(
                        keyword, city, category, employmentType, salaryMin, salaryMax,
                        shiftStartAfter, shiftEndBefore, status, authentication.getName(),
                        page, size, sortBy, direction
                )
        );
        return ResponseEntity.ok(jobPage.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.getJobById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody CreateJobRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.updateJob(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteJob(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.deleteJob(id, authentication.getName()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> closeJob(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.updateJobStatus(id, "CLOSED", authentication.getName()));
    }

    @PostMapping("/{id}/expire")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> expireJob(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.updateJobStatus(id, "EXPIRED", authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('EMPLOYER', 'ADMIN')")
    public ResponseEntity<JobResponse> patchJobStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.updateJobStatus(id, status, authentication.getName()));
    }

    @PostMapping("/{id}/bookmark")
    @PreAuthorize("hasAuthority('WORKER')")
    public ResponseEntity<Map<String, String>> bookmarkJob(
            @PathVariable UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(jobService.bookmarkJob(id, authentication.getName()));
    }
}
