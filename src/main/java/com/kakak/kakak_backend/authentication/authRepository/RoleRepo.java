package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepo extends JpaRepository<AuthRole, UUID> {
    Optional<AuthRole> findByName(String name);

}