package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectQueryService;
import com.hicct3.projectfinder.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;

    @Operation(summary = "프로젝트 목록 검색")
    @GetMapping
    public ApiResponse<Page<ProjectPreviewResponseDTO>> getProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(required = false) Long fieldId,
            Pageable pageable
    )
    {
        SortType sortType = SortType.from(sort);
        return ApiResponse.onSuccess(projectQueryService.getProjectList(sortType, keyword, pageable));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    private ApiResponse<ProjectDetailResponseDTO> getProjectDetail(@PathVariable Long projectId)
    {
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
    private ApiResponse<Void> updateProjectStatus(Authentication authentication, @PathVariable Long projectId, UpdateProjectStatusRequestDTO req) {
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
