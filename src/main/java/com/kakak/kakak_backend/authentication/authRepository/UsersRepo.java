package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepo extends JpaRepository<AuthUsers, UUID> {
    Optional<AuthUsers> findByEmail(String email);
    Optional<AuthUsers> findByPhone(String phone);
}
