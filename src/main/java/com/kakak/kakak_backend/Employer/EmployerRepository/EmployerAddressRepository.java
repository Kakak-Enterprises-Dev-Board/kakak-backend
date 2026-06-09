package com.kakak.kakak_backend.Employer.EmployerRepository;

import com.kakak.kakak_backend.Employer.EmployerEntity.Employer_Addresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployerAddressRepository extends JpaRepository<Employer_Addresses, UUID> {
    @Query("select a from Employer_Addresses a where a.employer_id.id = :employerId")
    Optional<Employer_Addresses> findByEmployerId(@Param("employerId") UUID employerId);
}
