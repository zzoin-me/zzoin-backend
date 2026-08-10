package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.project.MyReviewableProjectResponseDTO;
import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyApplicationPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyProjectPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.review.MyReviewsResponseDTO;
import com.hicct3.projectfinder.dto.project.review.WrittenReviewResponseDTO;
import com.hicct3.projectfinder.dto.user.*;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectService;
import com.hicct3.projectfinder.service.UserService;
import com.hicct3.projectfinder.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final ProjectService projectService;
    private final ReviewService reviewService;

    @Operation(summary = "내가 받은 리뷰 조회")
    @GetMapping("/me/reviews/received")
    public ApiResponse<MyReviewsResponseDTO> getMyReceivedReviews(
            Authentication authentication,
            Pageable pageable
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(
                "받은 리뷰 조회에 성공했습니다.",
                reviewService.getReceivedReviews(userDetails.getId(), pageable));
    }

    @Operation(summary = "내가 남긴 리뷰 조회")
    @GetMapping("/me/reviews/written")
    public ApiResponse<PageResponseDTO<WrittenReviewResponseDTO>> getMyWrittenReviews(
            Authentication authentication,
            Pageable pageable
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(
                "남긴 리뷰 조회에 성공했습니다.",
                PageResponseDTO.from(reviewService.getWrittenReviews(userDetails.getId(), pageable)));
    }

    @Operation(summary = "남겼거나 남길 수 있는 리뷰 조회")
    @GetMapping("/me/reviews/reviewable")
    public ApiResponse<PageResponseDTO<MyReviewableProjectResponseDTO>> getMyReviewableProjects(Authentication authentication, Pageable pageable) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(
                "작성 가능한 리뷰 조회에 성공했습니다.",
                PageResponseDTO.from(reviewService.getMyReviewableProjects(userDetails.getId(), pageable)));
    }

    @Operation(summary = "작성하지 않은 후기가 남은 프로젝트 수 조회")
    @GetMapping("/me/reviews/reviewable/pending-count")
    public ApiResponse<Long> getPendingReviewProjectCount(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(reviewService.getPendingReviewProjectCount(userDetails.getId()));
    }


    @Operation(summary = "내가 지원한 프로젝트 조회")
    @GetMapping("/me/applications")
    public ApiResponse<PageResponseDTO<MyApplicationPreviewResponseDTO>> getMyApplications(
            Authentication authentication,
            @RequestParam(required = false) ApplicationStatus status,
            Pageable pageable)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(PageResponseDTO.from(
                projectService.getMyApplications(userDetails.getId(), status, pageable)));
    }

    @Operation(summary = "내 프로젝트 조회")
    @GetMapping("/me/projects")
    public ApiResponse<PageResponseDTO<MyProjectPreviewResponseDTO>> getMyProjects(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean hasApplicants,
            Pageable pageable
    )
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(PageResponseDTO.from(projectService.getMyProjects(
                userDetails.getId(), status, hasApplicants, pageable)));
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
