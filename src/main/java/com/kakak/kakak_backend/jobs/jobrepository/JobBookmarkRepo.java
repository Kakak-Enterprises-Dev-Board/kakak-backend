package com.kakak.kakak_backend.jobs.jobrepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.jobs.jobentity.Job;
import com.kakak.kakak_backend.jobs.jobentity.JobBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobBookmarkRepo extends JpaRepository<JobBookmark, UUID> {
    Optional<JobBookmark> findByWorkerAndJob(AuthUsers worker, Job job);
    List<JobBookmark> findByWorker(AuthUsers worker);
}
