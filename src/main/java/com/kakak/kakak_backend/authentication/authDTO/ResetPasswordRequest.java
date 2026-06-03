package com.kakak.kakak_backend.authentication.authDTO;

import lombok.Data;

@Data
public class ResetPasswordRequest {

    private String phone;
    private String otp;
    private String newPassword;
}