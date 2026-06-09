package com.kakak.kakak_backend.profile.profileService;

import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import com.kakak.kakak_backend.authentication.authRepository.UsersRepo;
import com.kakak.kakak_backend.profile.profileDTO.ProfileImageRequest;
import com.kakak.kakak_backend.profile.profileDTO.UpdateUserProfileRequest;
import com.kakak.kakak_backend.profile.profileDTO.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {


    private final UsersRepo usersRepo;

    @Override
    public UserProfileResponse getProfile() {

        AuthUsers user = getCurrentUser();

        return mapToResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {

        AuthUsers user = getCurrentUser();

        if (request.getName() != null) {
            user.setUsername(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getExperience() != null) {
            user.setExperience(request.getExperience());
        }
        if (request.getSkills() != null) {
            user.setSkills(String.join(",", request.getSkills()));
        }
        usersRepo.save(user);

        return mapToResponse(user);
    }

    @Override
    public String uploadProfileImage(ProfileImageRequest request) {

        AuthUsers user = getCurrentUser();

        user.setProfileImageUrl(request.getProfileImageUrl());

        usersRepo.save(user);

        return "Profile image uploaded successfully";
    }

    private AuthUsers getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return usersRepo.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));
    }

    private UserProfileResponse mapToResponse(AuthUsers user) {

        UserProfileResponse response = new UserProfileResponse();

        response.setId(user.getId());
        response.setName(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setProfileImageUrl(user.getProfileImageUrl());

        if (user.getSkills() != null) {
            response.setSkills(
                    Arrays.asList(user.getSkills().split(","))
            );
        }

        response.setExperience(user.getExperience());
        response.setAverageRating(user.getAverageRating());
        response.setCreatedAt(user.getCreated_at().toInstant());
        response.setUpdatedAt(user.getUpdated_at().toInstant());
        return response;
    }
}