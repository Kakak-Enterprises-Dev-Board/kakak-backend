package com.kakak.kakak_backend.authentication.authDTO;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private String currentPassword;
    private String newPassword;
}