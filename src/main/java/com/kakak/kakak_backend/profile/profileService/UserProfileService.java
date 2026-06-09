package com.kakak.kakak_backend.profile.profileService;

import com.kakak.kakak_backend.profile.profileDTO.ProfileImageRequest;
import com.kakak.kakak_backend.profile.profileDTO.UpdateUserProfileRequest;
import com.kakak.kakak_backend.profile.profileDTO.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile();

    UserProfileResponse updateProfile(UpdateUserProfileRequest request);

    String uploadProfileImage(ProfileImageRequest request);
}