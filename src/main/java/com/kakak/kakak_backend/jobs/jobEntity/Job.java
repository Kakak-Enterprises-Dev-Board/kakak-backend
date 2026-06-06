package com.kakak.kakak_backend.jobs.jobEntity;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "jobs", schema = "kakak")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private AuthUsers employer;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "salary_type", length = 50)
    private String salaryType;

    @Column(name = "salary_amount", precision = 19, scale = 2)
    private BigDecimal salaryAmount;

    private Integer vacancies;

    @Column(name = "shift_start")
    private Timestamp shiftStart;

    @Column(name = "shift_end")
    private Timestamp shiftEnd;

    @Column(name = "location_name")
    private String locationName;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "radius_meters")
    private Integer radiusMeters;

    @Column(name = "gender_preference", length = 50)
    private String genderPreference;

    @Column(nullable = false, length = 50)
    private String status; // DRAFT, ACTIVE, CLOSED, EXPIRED

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_skills",
            schema = "kakak",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @Builder.Default
    private Set<Skill> requiredSkills = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return id != null && Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
