package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectQueryService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final UserRepository userRepository;

    @Operation(summary = "프로젝트 상세 조회")
    @Transactional
    public ProjectDetailResponseDTO getProjectDetail(Long projectId)
    {
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
            throw new GeneralException(ErrorCode.PROJECT_DELETED);

        return ProjectDetailResponseDTO.from(project, projectRecruitmentRepository.findByProject(project).stream().map(RecruitmentDetailResponseDTO::from).toList());
    }

    @Transactional
    public Page<ProjectPreviewResponseDTO> getProjectList(SortType sort, String keyword, Pageable pageable)
    {
        return projectRepository.searchProjects(sort, keyword, pageable).map(
                project -> ProjectPreviewResponseDTO.from(project,
                        projectRecruitmentRepository.findByProject(project))
        );
    }

}

