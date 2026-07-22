package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.project.myproject.MyApplicationPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyProjectPreviewResponseDTO;
import com.hicct3.projectfinder.dto.user.*;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectService;
import com.hicct3.projectfinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final ProjectService projectService;

    @Operation(summary = "내가 지원한 프로젝트 조회")
    @GetMapping("/me/applications")
    public ApiResponse<Page<MyApplicationPreviewResponseDTO>> getMyApplications(Authentication authentication, Pageable pageable)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(projectService.getMyApplications(userDetails.getId(), pageable));
    }

    @Operation(summary = "내 프로젝트 조회")
    @GetMapping("/me/projects")
    public ApiResponse<Page<MyProjectPreviewResponseDTO>> getMyProjects(
            Authentication authentication,
            Pageable pageable
    )
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(projectService.getMyProjects(userDetails.getId(), pageable));
    }

    @Operation(summary = "userId로 프로필 조회")
    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponseDTO> getUserProfile(
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                userService.getUserProfile(userId)
        );
    }

    @Operation(summary = "userId로 학교 프로필 조회")
    @GetMapping("/{userId}/school-profile")
    public ApiResponse<UserSchoolProfileResponseDTO> getUserSchoolProfile(
            @PathVariable Long userId
    ) {
        return ApiResponse.onSuccess(
                userService.getUserSchoolProfile(userId)
        );
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ApiResponse<MyProfileResponseDTO> getMyProfile(
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ApiResponse.onSuccess(
                userService.getMyProfile(userDetails.getId())
        );
    }

    @Operation(summary = "내 학교 프로필 조회")
    @GetMapping("/me/school-profile")
    public ApiResponse<UserSchoolProfileResponseDTO> getMySchoolProfile(
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ApiResponse.onSuccess(
                userService.getUserSchoolProfile(userDetails.getId())
        );
    }

    @Operation(summary = "내 프로필 수정")
    @PatchMapping("/me/profile")
    public ApiResponse<Void> updateProfile(Authentication authentication,
                                            @RequestBody @Valid UpdateProfileRequestDTO request)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        userService.updateProfile(userDetails.getId(), request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "내 학교 프로필 수정")
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
