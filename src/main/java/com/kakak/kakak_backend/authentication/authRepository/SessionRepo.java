package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthSession;
import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepo
        extends JpaRepository<AuthSession, UUID> {
    @Query("""
SELECT s
FROM AuthSession s
WHERE s.refresh_token = :token
""")
    Optional<AuthSession> findByRefreshToken(
            @Param("token") String token);

    List<AuthSession> findByUser(AuthUsers user);
}