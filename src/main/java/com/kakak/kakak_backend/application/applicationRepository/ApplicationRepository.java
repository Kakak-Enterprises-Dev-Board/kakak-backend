package com.kakak.kakak_backend.application.applicationRepository;

import com.kakak.kakak_backend.application.applicationEntity.Application;
import com.kakak.kakak_backend.application.applicationEntity.ApplicationStatus;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.jobs.jobentity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByWorker(AuthUsers worker);

    List<Application> findByWorkerAndStatus(
            AuthUsers worker,
            ApplicationStatus status
    );

    Optional<Application> findByJobAndWorker(
            Job job,
            AuthUsers worker
    );
}