package com.kakak.kakak_backend.jobs.jobrepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.jobs.jobentity.JobDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobDraftRepo extends JpaRepository<JobDraft, UUID> {
    List<JobDraft> findByEmployer(AuthUsers employer);
}
