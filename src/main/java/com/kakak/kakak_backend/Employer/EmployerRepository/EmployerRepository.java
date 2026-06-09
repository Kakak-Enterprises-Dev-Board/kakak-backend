package com.kakak.kakak_backend.Employer.EmployerRepository;

import com.kakak.kakak_backend.Employer.EmployerEntity.Employer;
import com.kakak.kakak_backend.Employer.EmployerEnum.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, UUID> {
    @Query("select e from Employer e where e.user_id.id = :userId order by e.created_at desc")
    List<Employer> findByUserId(@Param("userId") UUID userId);

    List<Employer> findByVerificationStatus(VerificationStatus verificationStatus);
}
