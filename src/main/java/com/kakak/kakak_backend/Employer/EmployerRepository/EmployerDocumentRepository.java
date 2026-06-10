package com.kakak.kakak_backend.Employer.EmployerRepository;

import com.kakak.kakak_backend.Employer.EmployerEntity.Employer_Documents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployerDocumentRepository extends JpaRepository<Employer_Documents, UUID> {
    @Query("select d from Employer_Documents d where d.employer_id.id = :employerId")
    List<Employer_Documents> findByEmployerId(@Param("employerId") UUID employerId);
    @Query("select d from Employer_Documents d where d.employer_id.id = :employerId order by d.uploaded_at desc limit 1")
    Optional<Employer_Documents>
    findTopByEmployer_id_IdOrderByUploaded_atDesc(@Param("employerId") UUID employerId);
}
