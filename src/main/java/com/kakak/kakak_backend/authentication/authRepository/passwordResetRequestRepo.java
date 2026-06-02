package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface passwordResetRequestRepo extends JpaRepository<PasswordResetRequest, UUID> {
}
