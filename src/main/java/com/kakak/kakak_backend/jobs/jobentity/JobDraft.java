package com.kakak.kakak_backend.jobs.jobentity;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "job_drafts", schema = "kakak")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private AuthUsers employer;

    @Column(name = "draft_payload", columnDefinition = "TEXT", nullable = false)
    private String draftPayload;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDraft jobDraft = (JobDraft) o;
        return id != null && Objects.equals(id, jobDraft.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
