package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import com.hicct3.projectfinder.dto.project.ProjectPreviewResponseDTO;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-feeds")
@RequiredArgsConstructor
public class ProjectFeedController {

    private final ProjectQueryService projectQueryService;

    @Operation(summary = "인기 프로젝트 목록")
    @GetMapping("/popular")
    public ApiResponse<PageResponseDTO<ProjectPreviewResponseDTO>> getPopularProjects(Pageable pageable) {
        return ApiResponse.onSuccess(PageResponseDTO.from(
                projectQueryService.getPopularProjects(pageable)));
    }

    @Operation(summary = "추천 프로젝트 목록")
    @GetMapping("/recommend")
    public ApiResponse<PageResponseDTO<ProjectPreviewResponseDTO>> getRecommendProjects(
            Authentication authentication, Pageable pageable) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(PageResponseDTO.from(
                projectQueryService.getRecommendProjects(userDetails.getId(), pageable)));
    }
}
