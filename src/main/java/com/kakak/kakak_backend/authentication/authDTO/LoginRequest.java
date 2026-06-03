package com.kakak.kakak_backend.authentication.authDTO;

import lombok.Data;

@Data
public class LoginRequest {
    private String phone;
    private String password;
}