package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthSession;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepo
        extends JpaRepository<AuthSession, UUID> {

    List<AuthSession> findByUser(AuthUsers user);
}