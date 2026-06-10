package com.kakak.kakak_backend.profile.profileDTO;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class UserProfileResponse {

    private UUID id;

    private String name;

    private String email;

    private String phone;

    private String profileImageUrl;

    private List<String> skills;

    private String experience;

    private Double averageRating;

    private Instant createdAt;

    private Instant updatedAt;
}