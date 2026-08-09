package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.jobrole.JobCategoryResponseDTO;
import com.hicct3.projectfinder.dto.jobrole.JobRoleResponseDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.service.JobRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/job-roles")
public class JobRoleController {

    private final JobRoleService jobRoleService;

    @GetMapping("/categories")
    public ApiResponse<List<JobCategoryResponseDTO>> getJobCategories() {
        return ApiResponse.onSuccess(jobRoleService.getJobCategories());
    }

    @GetMapping
    public ApiResponse<List<JobRoleResponseDTO>> getJobRoles(
            @RequestParam(value = "category", required = false) String category
    ) {
        return ApiResponse.onSuccess(jobRoleService.getJobRoles(category));
    }
}
