package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.jobrole.JobCategoryResponseDTO;
import com.hicct3.projectfinder.dto.jobrole.JobRoleResponseDTO;
import com.hicct3.projectfinder.entity.JobCategory;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.JobCategoryRepository;
import com.hicct3.projectfinder.repository.JobRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRoleService {

    private final JobCategoryRepository jobCategoryRepository;
    private final JobRoleRepository jobRoleRepository;

    @Transactional(readOnly = true)
    public List<JobCategoryResponseDTO> getJobCategories() {
        return jobCategoryRepository.findAll().stream()
                .map(JobCategoryResponseDTO::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobRoleResponseDTO> getJobRoles(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return jobRoleRepository.findAll().stream()
                    .map(JobRoleResponseDTO::of)
                    .toList();
        }

        JobCategoryCode code = parseCategoryCode(categoryCode);
        JobCategory category = jobCategoryRepository.findByCategoryCode(code)
                .orElseThrow(() -> new GeneralException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        return jobRoleRepository.findAllByJobCategory(category).stream()
                .map(JobRoleResponseDTO::of)
                .toList();
    }

    private JobCategoryCode parseCategoryCode(String categoryCode) {
        try {
            return JobCategoryCode.valueOf(categoryCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ErrorCode.JOB_CATEGORY_NOT_FOUND);
        }
    }
}
