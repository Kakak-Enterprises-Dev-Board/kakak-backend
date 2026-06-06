package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthOtp_logs;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpLogsRepo extends JpaRepository<AuthOtp_logs, UUID> {
    @Query("select o from AuthOtp_logs o where o.phone = :phone and o.purpose = :purpose and o.verified = false order by o.created_at desc")
    List<AuthOtp_logs> findLatestUnverifiedOtpList(@Param("phone") String phone, @Param("purpose") String purpose, Pageable pageable);

    default Optional<AuthOtp_logs> findLatestUnverifiedOtp(String phone, String purpose) {
        List<AuthOtp_logs> list = findLatestUnverifiedOtpList(phone, purpose, PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}

