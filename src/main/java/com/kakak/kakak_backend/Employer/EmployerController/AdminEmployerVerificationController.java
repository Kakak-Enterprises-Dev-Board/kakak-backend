package com.kakak.kakak_backend.Employer.EmployerController;

import com.kakak.kakak_backend.Employer.EmployerDTO.AdminEmployerVerificationResponse;
import com.kakak.kakak_backend.Employer.EmployerService.AdminEmployerVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employers")
@RequiredArgsConstructor
public class AdminEmployerVerificationController {

    private final AdminEmployerVerificationService adminEmployerVerificationService;

    @GetMapping("/pending")
    public ResponseEntity<List<AdminEmployerVerificationResponse>> getPendingEmployers() {
        return ResponseEntity.ok(adminEmployerVerificationService.getPendingEmployers());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminEmployerVerificationResponse> approveEmployer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminEmployerVerificationService.approveEmployer(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminEmployerVerificationResponse> rejectEmployer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminEmployerVerificationService.rejectEmployer(id));
    }
}
