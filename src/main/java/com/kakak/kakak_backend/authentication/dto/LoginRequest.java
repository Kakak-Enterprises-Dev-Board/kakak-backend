package com.kakak.kakak_backend.authentication.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @JsonProperty("username")
    @JsonAlias({"identifier", "login", "email"})
    @NotBlank(message = "Username or email is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Boolean rememberMe = Boolean.FALSE;
}
