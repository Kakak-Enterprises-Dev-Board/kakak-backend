package com.kakak.kakak_backend.jobs.jobService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import com.kakak.kakak_backend.jobs.jobDTO.CreateJobRequest;
import com.kakak.kakak_backend.jobs.jobDTO.JobResponse;
import com.kakak.kakak_backend.jobs.jobEntity.Job;
import com.kakak.kakak_backend.jobs.jobEntity.JobBookmark;
import com.kakak.kakak_backend.jobs.jobEntity.JobDraft;
import com.kakak.kakak_backend.jobs.jobEntity.Skill;
import com.kakak.kakak_backend.jobs.jobRepository.JobBookmarkRepo;
import com.kakak.kakak_backend.jobs.jobRepository.JobDraftRepo;
import com.kakak.kakak_backend.jobs.jobRepository.JobRepo;
import com.kakak.kakak_backend.jobs.jobRepository.SkillRepo;
import com.kakak.kakak_backend.jobs.jobSpecification.JobSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepo jobRepo;
    private final SkillRepo skillRepo;
    private final JobDraftRepo jobDraftRepo;
    private final JobBookmarkRepo jobBookmarkRepo;
    private final UsersRepo usersRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public JobResponse createJob(CreateJobRequest request, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        Job job = Job.builder()
                .employer(employer)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .employmentType(request.getEmploymentType())
                .salaryAmount(request.getSalary())
                .salaryType(request.getSalaryType())
                .vacancies(request.getVacancies())
                .shiftStart(request.getStartDate())
                .shiftEnd(request.getEndDate())
                .locationName(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusMeters(request.getRadiusMeters())
                .genderPreference(request.getGenderPreference())
                .status("ACTIVE")
                .requiredSkills(resolveSkills(request.getRequiredSkills()))
                .build();

        Job savedJob = jobRepo.save(job);
        return mapToJobResponse(savedJob);
    }

    @Transactional
    public Map<String, Object> saveDraft(CreateJobRequest request, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        try {
            String jsonPayload = objectMapper.writeValueAsString(request);
            JobDraft draft = JobDraft.builder()
                    .employer(employer)
                    .draftPayload(jsonPayload)
                    .build();

            JobDraft savedDraft = jobDraftRepo.save(draft);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job draft saved successfully");
            response.put("draftId", savedDraft.getId());
            return response;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize draft payload", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save draft");
        }
    }

    @Transactional
    public JobResponse publishDraft(UUID draftId, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        JobDraft draft = jobDraftRepo.findById(draftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));

        validateOwnership(draft.getEmployer().getId(), employer);

        try {
            CreateJobRequest request = objectMapper.readValue(draft.getDraftPayload(), CreateJobRequest.class);
            validateDraftRequest(request);

            Job job = Job.builder()
                    .employer(employer)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .category(request.getCategory())
                    .employmentType(request.getEmploymentType())
                    .salaryAmount(request.getSalary())
                    .salaryType(request.getSalaryType())
                    .vacancies(request.getVacancies())
                    .shiftStart(request.getStartDate())
                    .shiftEnd(request.getEndDate())
                    .locationName(request.getCity())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .radiusMeters(request.getRadiusMeters())
                    .genderPreference(request.getGenderPreference())
                    .status("ACTIVE")
                    .requiredSkills(resolveSkills(request.getRequiredSkills()))
                    .build();

            Job savedJob = jobRepo.save(job);
            jobDraftRepo.delete(draft);

            return mapToJobResponse(savedJob);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize draft payload", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse draft payload");
        }
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id, String userEmail) {
        AuthUsers user = getUserByEmail(userEmail);
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if ("WORKER".equalsIgnoreCase(user.getRole_id().getName()) && !"ACTIVE".equalsIgnoreCase(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        return mapToJobResponse(job);
    }

    @Transactional
    public JobResponse updateJob(UUID id, CreateJobRequest request, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        validateOwnership(job.getEmployer().getId(), employer);

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setLocationName(request.getCity());
        job.setCategory(request.getCategory());
        job.setEmploymentType(request.getEmploymentType());
        job.setSalaryAmount(request.getSalary());
        job.setSalaryType(request.getSalaryType());
        job.setVacancies(request.getVacancies());
        job.setShiftStart(request.getStartDate());
        job.setShiftEnd(request.getEndDate());
        job.setLatitude(request.getLatitude());
        job.setLongitude(request.getLongitude());
        job.setRadiusMeters(request.getRadiusMeters());
        job.setGenderPreference(request.getGenderPreference());

        if (request.getRequiredSkills() != null) {
            job.getRequiredSkills().clear();
            job.getRequiredSkills().addAll(resolveSkills(request.getRequiredSkills()));
        }

        Job updatedJob = jobRepo.save(job);
        return mapToJobResponse(updatedJob);
    }

    @Transactional
    public Map<String, String> deleteJob(UUID id, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        validateOwnership(job.getEmployer().getId(), employer);

        jobRepo.delete(job);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Job deleted successfully");
        return response;
    }

    @Transactional
    public JobResponse updateJobStatus(UUID id, String newStatus, String employerEmail) {
        AuthUsers employer = getUserByEmail(employerEmail);
        validateEmployerOrAdmin(employer);

        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        validateOwnership(job.getEmployer().getId(), employer);

        String upperStatus = newStatus.toUpperCase();
        if (!Arrays.asList("DRAFT", "ACTIVE", "CLOSED", "EXPIRED").contains(upperStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
        }

        job.setStatus(upperStatus);
        Job savedJob = jobRepo.save(job);
        return mapToJobResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchAndFilterJobs(
            String keyword,
            String city,
            String category,
            String employmentType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            Timestamp shiftStartAfter,
            Timestamp shiftEndBefore,
            String status,
            String userEmail,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        AuthUsers user = getUserByEmail(userEmail);
        String enforcedStatus = status;

        if ("WORKER".equalsIgnoreCase(user.getRole_id().getName())) {
            enforcedStatus = "ACTIVE"; // Workers are strictly forbidden from seeing non-active jobs
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        String mappedSortKey = mapSortKey(sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, mappedSortKey));

        Specification<Job> spec = JobSpecification.filterJobs(
                keyword,
                city,
                category,
                employmentType,
                salaryMin,
                salaryMax,
                shiftStartAfter,
                shiftEndBefore,
                enforcedStatus,
                null
        );

        Page<Job> jobPage = jobRepo.findAll(spec, pageable);
        return jobPage.map(this::mapToJobResponse);
    }

    @Transactional
    public Map<String, String> bookmarkJob(UUID jobId, String workerEmail) {
        AuthUsers worker = getUserByEmail(workerEmail);
        if (!"WORKER".equalsIgnoreCase(worker.getRole_id().getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only workers can bookmark jobs");
        }

        Job job = jobRepo.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!"ACTIVE".equalsIgnoreCase(job.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot bookmark an inactive job");
        }

        Optional<JobBookmark> existingBookmark = jobBookmarkRepo.findByWorkerAndJob(worker, job);
        Map<String, String> response = new HashMap<>();

        if (existingBookmark.isPresent()) {
            jobBookmarkRepo.delete(existingBookmark.get());
            response.put("message", "Job bookmark removed successfully");
            response.put("bookmarked", "false");
        } else {
            JobBookmark bookmark = JobBookmark.builder()
                    .worker(worker)
                    .job(job)
                    .build();
            jobBookmarkRepo.save(bookmark);
            response.put("message", "Job bookmarked successfully");
            response.put("bookmarked", "true");
        }

        return response;
    }

    // --- Helper Methods ---

    private AuthUsers getUserByEmail(String email) {
        return usersRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void validateEmployerOrAdmin(AuthUsers user) {
        String role = user.getRole_id().getName();
        if (!"EMPLOYER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Insufficient permissions");
        }
    }

    private void validateOwnership(UUID ownerId, AuthUsers currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole_id().getName())) {
            return; // Admins bypass ownership checks
        }
        if (!ownerId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this listing");
        }
    }

    private void validateDraftRequest(CreateJobRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publishing failed: Title is required in draft");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publishing failed: Description is required in draft");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publishing failed: City/Location is required in draft");
        }
        if (request.getSalary() == null || request.getSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publishing failed: Valid salary is required in draft");
        }
    }

    private String mapSortKey(String sortBy) {
        if (sortBy == null) {
            return "createdAt";
        }
        switch (sortBy.toLowerCase()) {
            case "wage":
                return "salaryAmount";
            case "time":
                return "shiftStart";
            case "createdat":
            default:
                return "createdAt";
        }
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        Set<Skill> skills = new HashSet<>();
        if (skillNames != null) {
            for (String name : skillNames) {
                if (name == null || name.isBlank()) continue;
                String trimmedName = name.trim();
                Skill skill = skillRepo.findByName(trimmedName)
                        .orElseGet(() -> skillRepo.save(Skill.builder().name(trimmedName).build()));
                skills.add(skill);
            }
        }
        return skills;
    }

    private JobResponse mapToJobResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .employerId(job.getEmployer().getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .city(job.getLocationName())
                .category(job.getCategory())
                .employmentType(job.getEmploymentType())
                .salary(job.getSalaryAmount())
                .salaryType(job.getSalaryType())
                .vacancies(job.getVacancies())
                .status(job.getStatus())
                .requiredSkills(job.getRequiredSkills().stream().map(Skill::getName).toList())
                .shiftStart(job.getShiftStart())
                .shiftEnd(job.getShiftEnd())
                .locationName(job.getLocationName())
                .latitude(job.getLatitude())
                .longitude(job.getLongitude())
                .radiusMeters(job.getRadiusMeters())
                .genderPreference(job.getGenderPreference())
                .expiresAt(job.getExpiresAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
