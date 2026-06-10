package com.kakak.kakak_backend.application.applicationService;

import com.kakak.kakak_backend.application.applicationDTO.JobHistoryResponse;
import com.kakak.kakak_backend.application.applicationEntity.Application;
import com.kakak.kakak_backend.application.applicationEntity.ApplicationStatus;
import com.kakak.kakak_backend.application.applicationRepository.ApplicationRepository;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UsersRepo usersRepo;

    @Override
    public List<JobHistoryResponse> getUserJobHistory(String status) {

        AuthUsers user = getCurrentUser();

        List<Application> applications;

        if (status != null) {

            applications = applicationRepository
                    .findByWorkerAndStatus(
                            user,
                            ApplicationStatus.valueOf(status)
                    );

        } else {

            applications =
                    applicationRepository.findByWorker(user);
        }

        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private JobHistoryResponse mapToResponse(
            Application application) {

        JobHistoryResponse response =
                new JobHistoryResponse();

        response.setApplicationId(
                application.getId());

        response.setJobId(
                application.getJob().getId());

        response.setStatus(
                application.getStatus().name());

        if (application.getCompletedAt() != null) {
            response.setCompletedAt(
                    application.getCompletedAt().toInstant());
        }

        return response;
    }

    private AuthUsers getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        return usersRepo.findByEmail(email)
                .orElseThrow();
    }
}