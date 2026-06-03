package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthOtp_logs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpLogsRepo extends JpaRepository<AuthOtp_logs, UUID> {
    @Query(value = "select * from otp_logs where phone = :phone and purpose = :purpose and verified = false order by created_at desc limit 1", nativeQuery = true)
    Optional<AuthOtp_logs> findLatestUnverifiedOtp(String phone, String purpose);
}
