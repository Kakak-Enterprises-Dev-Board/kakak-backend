package com.kakak.kakak_backend.authentication.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String role;
    private String status;
    private boolean phoneVerified;
    private boolean emailVerified;
    private Timestamp createdAt;
}