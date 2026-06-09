package com.kakak.kakak_backend.profile.profileDTO;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserProfileRequest {

    private String name;

    private String email;

    private String phone;

    private List<String> skills;

    private String experience;
}