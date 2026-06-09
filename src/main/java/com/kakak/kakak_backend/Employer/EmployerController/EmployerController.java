package com.kakak.kakak_backend.Employer.EmployerController;

import com.kakak.kakak_backend.Employer.EmployerDTO.*;
import com.kakak.kakak_backend.Employer.EmployerService.EmployerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employers")
@RequiredArgsConstructor
public class EmployerController {

    private final EmployerService employerService;

    @GetMapping("/dashboard")
    public ResponseEntity<EmployerDashboardResponse> getEmployerDashboard(
            Authentication authentication) {

        return ResponseEntity.ok(
                employerService.getEmployerDashboard(authentication)
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<EmployerProfileResponse> getEmployerProfile(
            Authentication authentication) {
        return ResponseEntity.ok(
                employerService.getEmployerProfile(authentication)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicEmployerResponse> getEmployer(@PathVariable UUID id) {
        return ResponseEntity.ok(employerService.getPublicEmployer(id));
    }

    @PutMapping("/profile")
    public ResponseEntity<EmployerProfileResponse> updateEmployerProfile(
            @Valid @RequestBody UpdateEmployerProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(employerService.updateEmployerProfile(request, authentication));
    }

    @PostMapping("/address")
    public ResponseEntity<EmployerProfileResponse> upsertEmployerAddress(
            @Valid @RequestBody EmployerAddressRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(employerService.upsertEmployerAddress(request, authentication));
    }

    @PostMapping("/documents")
    public ResponseEntity<EmployerProfileResponse> uploadEmployerDocument(
            @Valid @RequestBody EmployerDocumentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(employerService.uploadEmployerDocument(request, authentication));
    }
}
