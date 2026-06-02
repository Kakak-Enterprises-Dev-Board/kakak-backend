package com.kakak.kakak_backend.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshTokenResponse {
    private String accessToken;
    private long expiresIn;
    private String tokenType;
}
