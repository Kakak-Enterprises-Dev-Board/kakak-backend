package com.kakak.kakak_backend.jobs.jobrepository;

import com.kakak.kakak_backend.jobs.jobentity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepo extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByName(String name);
}
