package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.application.ApplyProjectRequestDTO;
import com.hicct3.projectfinder.dto.application.DeleteProjectRequestDTO;
import com.hicct3.projectfinder.dto.application.ProjectApplicantsResponseDTO;
import com.hicct3.projectfinder.dto.application.UpdateApplicantStatusDTO;
import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.dto.project.review.CreateReviewRequestDTO;
import com.hicct3.projectfinder.dto.project.review.MemberReviewsResponseDTO;
import com.hicct3.projectfinder.dto.project.review.MembersResponseDTO;
import com.hicct3.projectfinder.dto.project.review.ReviewTargetsResponseDTO;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectApplicationService;
import com.hicct3.projectfinder.service.ProjectQueryService;
import com.hicct3.projectfinder.service.ProjectService;
import com.hicct3.projectfinder.service.ReviewService;
import com.hicct3.projectfinder.service.SecurityRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;
    private final ProjectApplicationService projectApplicationService;
    private final ReviewService reviewService;
    private final SecurityRateLimitService rateLimitService;

    @Operation(summary = "팀원 목록 조회")
    @GetMapping("/{projectId}/members")
    public ApiResponse<MembersResponseDTO> getMembers(Authentication authentication, @PathVariable Long projectId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess("팀원 목록 조회에 성공했습니다.", reviewService.getMembers(userDetails.getId(), projectId));
    }

    @Operation(summary = "팀원 평가 대상 조회")
    @GetMapping("/{projectId}/review-targets")
    public ApiResponse<ReviewTargetsResponseDTO> getReviewTargets(
            Authentication authentication,
            @PathVariable Long projectId
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(
                "팀원 평가 대상 조회에 성공했습니다.",
                reviewService.getReviewTargets(userDetails.getId(), projectId));
    }

    @Operation(summary = "팀원 평가 상세 조회")
    @GetMapping("/{projectId}/reviews")
    public ApiResponse<MemberReviewsResponseDTO> getMemberReviews(Authentication authentication, @PathVariable Long projectId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess("팀원 평가 상세 조회에 성공했습니다.", reviewService.getMyProjectReviews(userDetails.getId(), projectId));
    }

    @Operation(summary = "팀원 평가 등록")
    @PostMapping("/{projectId}/reviews")
    public ApiResponse<Void> createMemberReview(Authentication authentication, @PathVariable Long projectId, @RequestBody @Valid CreateReviewRequestDTO req)
    {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();
        reviewService.createReview(userDetails.getId(), projectId, req);
        return ApiResponse.onSuccess("팀원 평가 등록에 성공했습니다.", null);
    }

    @Operation(summary = "지원자 상태 변경")
    @PatchMapping("applications/{applicationId}")
    public ApiResponse<Void> updateApplicantStatus(Authentication authentication, @PathVariable Long applicationId, @RequestBody @Valid UpdateApplicantStatusDTO req) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectApplicationService.updateApplicantStatus(userDetails.getId(), applicationId, req);
        return ApiResponse.onSuccess("지원자 상태 변경에 성공했습니다.", null);
    }

    @Operation(summary = "프로젝트 지원")
    @PostMapping("/apply")
    public ApiResponse<Void> applyProject(Authentication authentication,  @RequestBody @Valid ApplyProjectRequestDTO req) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectApplicationService.applyProject(userDetails.getId(), req);
        return ApiResponse.onSuccess("프로젝트 지원에 성공했습니다.", null);
    }

    @Operation(summary = "프로젝트 지원 취소")
    @DeleteMapping("/apply")
    public ApiResponse<Void> deleteApplication(Authentication authentication,  @RequestBody @Valid DeleteProjectRequestDTO req) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectApplicationService.deleteApplication(userDetails.getId(), req);
        return ApiResponse.onSuccess("프로젝트 지원 취소에 성공했습니다.", null);
    }

    @Operation(summary = "카테고리별 프로젝트 수")
    @GetMapping("/category-counts")
    public ApiResponse<Map<JobCategoryCode, Long>> getCategoryCounts()
    {
        return ApiResponse.onSuccess(projectQueryService.countProjectsPerCategory());
    }

    @Operation(summary = "프로젝트 지원자 목록 조회")
    @GetMapping("{projectId}/applicants")
    public ApiResponse<ProjectApplicantsResponseDTO> getApplicants(Authentication authentication, @PathVariable Long projectId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return ApiResponse.onSuccess(projectApplicationService.getApplicants(userDetails.getId(), projectId));
    }

    @Operation(summary = "프로젝트 목록 검색")
    @GetMapping
    public ApiResponse<PageResponseDTO<ProjectPreviewResponseDTO>> getProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) JobCategoryCode category,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) Integer minCount,
            @RequestParam(required = false) Integer maxCount,
            @RequestParam(required = false) GoalType goal,
            @RequestParam(required = false, defaultValue = "false") Boolean recruitingOnly,
            Pageable pageable
    )
    {
        SortType sortType = SortType.from(sort);
        return ApiResponse.onSuccess(PageResponseDTO.from(
                projectQueryService.getProjectList(sortType, keyword, category, name,
                        maxDays, minCount, maxCount, goal, recruitingOnly, pageable)));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    private ApiResponse<ProjectDetailResponseDTO> getProjectDetail(
            HttpServletRequest request,
            @PathVariable Long projectId)
    {
        String remoteAddress = request.getRemoteAddr();
        rateLimitService.consume(
                "project-view",
                remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress,
                120,
                Duration.ofMinutes(1));
        return ApiResponse.onSuccess("프로젝트 상세 조회 성공했습니다.", projectQueryService.getProjectDetail(projectId));
    }

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    private ApiResponse<Void> createProject(
            Authentication authentication,
            @Valid @RequestBody CreateProjectRequestDTO req) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectService.createProject(userDetails.getId(), req);
        return ApiResponse.onSuccess("프로젝트 등록 성공했습니다.", null);
    }

    @Operation(summary = "프로젝트 수정")
    @PatchMapping("/{projectId}")
    private ApiResponse<Void> updateProject(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequestDTO req) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectService.updateProject(userDetails.getId(), projectId, req);
        return ApiResponse.onSuccess("프로젝트 수정 성공했습니다.", null);
    }

    @Operation(summary = "프로젝트 상태 변경")
    @PatchMapping("/{projectId}/status")
    private ApiResponse<Void> updateProjectStatus(Authentication authentication, @PathVariable Long projectId, @Valid @RequestBody UpdateProjectStatusRequestDTO req) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectService.setProjectStatus(userDetails.getId(), projectId, req);
        return ApiResponse.onSuccess("프로젝트 상태 변경 성공했습니다.", null);
    }

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    private ApiResponse<Void> deleteProject(
            Authentication authentication,
            @PathVariable Long projectId) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        projectService.deleteProject(userDetails.getId(), projectId);
        return ApiResponse.onSuccess("프로젝트 삭제 성공했습니다.", null);
    }
}
