package com.kakak.kakak_backend.authentication.authRepository;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authEntity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustedDeviceRepo
        extends JpaRepository<TrustedDevice, UUID> {

    List<TrustedDevice> findByUser(AuthUsers user);
}