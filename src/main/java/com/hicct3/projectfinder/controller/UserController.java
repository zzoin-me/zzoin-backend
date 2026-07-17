package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.user.UpdateProfileRequestDTO;
import com.hicct3.projectfinder.dto.user.UpdateSchoolProfileRequestDTO;
import com.hicct3.projectfinder.dto.user.UserProfileResponseDTO;
import com.hicct3.projectfinder.dto.user.UserSchoolProfileResponseDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponseDTO> getUserProfile(
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                userService.getUserProfile(userId)
        );
    }

    @GetMapping("/{userId}/school-profile")
    public ApiResponse<UserSchoolProfileResponseDTO> getUserSchoolProfile(
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                userService.getUserSchoolProfile(userId)
        );
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponseDTO> getMyProfile(
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ApiResponse.onSuccess(
                userService.getMyProfile(userDetails.getId())
        );
    }

    @PatchMapping("/me/profile")
    public ApiResponse<Void> updateProfile(Authentication authentication,
                                            @RequestBody @Valid UpdateProfileRequestDTO request)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        userService.updateProfile(userDetails.getId(), request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/me/school-profile")
    public ApiResponse<Void> updateSchoolProfile(Authentication authentication,
                                           @RequestBody @Valid UpdateSchoolProfileRequestDTO request)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        userService.updateSchoolProfile(userDetails.getId(), request);
        return ApiResponse.onSuccess(null);
    }
}
