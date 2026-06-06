package com.kakak.kakak_backend.jobs.jobrepository;

import com.kakak.kakak_backend.jobs.jobentity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepo extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
}
