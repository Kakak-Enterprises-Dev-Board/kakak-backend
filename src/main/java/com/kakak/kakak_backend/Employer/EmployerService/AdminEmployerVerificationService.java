package com.kakak.kakak_backend.Employer.EmployerService;

import com.kakak.kakak_backend.Employer.EmployerDTO.AdminEmployerVerificationResponse;
import com.kakak.kakak_backend.Employer.EmployerEntity.Employer;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import com.kakak.kakak_backend.Employer.EmployerRepository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminEmployerVerificationService {

    private final EmployerRepository employerRepository;

    @Transactional(readOnly = true)
    public List<AdminEmployerVerificationResponse> getPendingEmployers() {
        return employerRepository.findByVerificationStatus(VerificationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminEmployerVerificationResponse approveEmployer(UUID employerId) {
        Employer employer = findEmployer(employerId);
        employer.setVerificationStatus(VerificationStatus.APPROVED);
        return toResponse(employerRepository.save(employer));
    }

    @Transactional
    public AdminEmployerVerificationResponse rejectEmployer(UUID employerId) {
        Employer employer = findEmployer(employerId);
        employer.setVerificationStatus(VerificationStatus.REJECTED);
        return toResponse(employerRepository.save(employer));
    }

    private Employer findEmployer(UUID employerId) {
        return employerRepository.findById(employerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employer not found"));
    }

    private AdminEmployerVerificationResponse toResponse(Employer employer) {
        return new AdminEmployerVerificationResponse(
                employer.getId(),
                employer.getCompany_name(),
                employer.getVerificationStatus(),
                employer.getCreated_at()
        );
    }
}
