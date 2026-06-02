package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthSession;
import com.kakak.kakak_backend.authentication.authEntity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface authSessionRepo extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByIdAndRevokedFalse(UUID id);

    List<AuthSession> findAllByUserAndRevokedFalseOrderByCreatedAtDesc(User user);

    List<AuthSession> findAllByUser(User user);
}
