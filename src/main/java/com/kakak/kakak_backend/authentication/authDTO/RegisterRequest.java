package com.kakak.kakak_backend.authentication.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String password;
    private String confirmPassword;
    private String role;
}
