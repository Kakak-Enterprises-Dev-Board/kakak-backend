package com.kakak.kakak_backend.profile.profileController;

import com.kakak.kakak_backend.profile.profileDTO.ProfileImageRequest;
import com.kakak.kakak_backend.profile.profileDTO.UpdateUserProfileRequest;
import com.kakak.kakak_backend.profile.profileDTO.UserProfileResponse;
import com.kakak.kakak_backend.profile.profileService.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public UserProfileResponse getProfile() {

        return userProfileService.getProfile();
    }

    @PutMapping
    public UserProfileResponse updateProfile(
            @RequestBody UpdateUserProfileRequest request) {

        return userProfileService.updateProfile(request);
    }

    @PostMapping("/image")
    public String uploadProfileImage(
            @RequestBody ProfileImageRequest request) {

        return userProfileService.uploadProfileImage(request);
    }
}