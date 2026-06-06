package com.kakak.kakak_backend.jobs;

import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import com.kakak.kakak_backend.jobs.jobdto.CreateJobRequest;
import com.kakak.kakak_backend.jobs.jobdto.JobResponse;
import com.kakak.kakak_backend.jobs.jobentity.Job;
import com.kakak.kakak_backend.jobs.jobentity.Skill;
import com.kakak.kakak_backend.jobs.jobrepository.JobBookmarkRepo;
import com.kakak.kakak_backend.jobs.jobrepository.JobDraftRepo;
import com.kakak.kakak_backend.jobs.jobrepository.JobRepo;
import com.kakak.kakak_backend.jobs.jobrepository.SkillRepo;
import com.kakak.kakak_backend.jobs.jobservice.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    private JobService jobService;

    @Mock
    private JobRepo jobRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private JobDraftRepo jobDraftRepo;
    @Mock
    private JobBookmarkRepo jobBookmarkRepo;
    @Mock
    private UsersRepo usersRepo;

    private AuthUsers employer;
    private AuthUsers worker;
    private AuthRole employerRole;
    private AuthRole workerRole;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepo, skillRepo, jobDraftRepo, jobBookmarkRepo, usersRepo);

        employerRole = new AuthRole();
        employerRole.setName("EMPLOYER");

        workerRole = new AuthRole();
        workerRole.setName("WORKER");

        employer = new AuthUsers();
        employer.setId(UUID.randomUUID());
        employer.setEmail("employer@example.com");
        employer.setRole_id(employerRole);

        worker = new AuthUsers();
        worker.setId(UUID.randomUUID());
        worker.setEmail("worker@example.com");
        worker.setRole_id(workerRole);
    }

    @Test
    void testCreateJob_Success() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Software Developer")
                .description("Write code")
                .city("New York")
                .salary(BigDecimal.valueOf(100.0))
                .requiredSkills(Arrays.asList("Java", "Spring"))
                .build();

        when(usersRepo.findByEmail("employer@example.com")).thenReturn(Optional.of(employer));
        when(skillRepo.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return Optional.of(Skill.builder().name(name).build());
        });
        when(jobRepo.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        JobResponse response = jobService.createJob(request, "employer@example.com");

        assertNotNull(response);
        assertEquals("Software Developer", response.getTitle());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(2, response.getRequiredSkills().size());
        verify(jobRepo, times(1)).save(any(Job.class));
    }

    @Test
    void testCreateJob_ForbiddenForWorker() {
        CreateJobRequest request = CreateJobRequest.builder()
                .title("Software Developer")
                .description("Write code")
                .city("New York")
                .salary(BigDecimal.valueOf(100.0))
                .build();

        when(usersRepo.findByEmail("worker@example.com")).thenReturn(Optional.of(worker));

        assertThrows(ResponseStatusException.class, () -> {
            jobService.createJob(request, "worker@example.com");
        });
        verify(jobRepo, never()).save(any(Job.class));
    }
}
