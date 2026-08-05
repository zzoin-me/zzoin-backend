package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectQuestionRepository;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectQueryService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectQuestionRepository projectQuestionRepository;
    private final UserRepository userRepository;

    @Operation(summary = "프로젝트 상세 조회")
    @Transactional
    public ProjectDetailResponseDTO getProjectDetail(Long projectId)
    {
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
            throw new GeneralException(ErrorCode.PROJECT_DELETED);

        var recruitments = projectRecruitmentRepository.findByProject(project).stream().map(RecruitmentDetailResponseDTO::from).toList();
        var questions = projectQuestionRepository.findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project).stream().map(QuestionResponseDTO::from).toList();

        return ProjectDetailResponseDTO.from(project, recruitments, questions);
    }

    @Transactional(readOnly = true)
    public Page<ProjectPreviewResponseDTO> getProjectList(SortType sort, String keyword, List<RecruitmentCategory> categories, List<String> names, Integer maxDays, Integer minCount, Integer maxCount, List<GoalType> goals, Boolean recruitingOnly, Pageable pageable)
    {
        Page<Project> projects = projectRepository.searchProjects(sort, keyword, categories, names, maxDays, minCount, maxCount, goals, recruitingOnly, pageable);

        List<Project> content = projects.getContent();
        Map<Long, List<ProjectRecruitment>> recruitmentMap = projectRecruitmentRepository
                .findAllByProjectInAndDeletedAtIsNull(content)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getProject().getId()));

        List<ProjectPreviewResponseDTO> dtos = content.stream().map(project -> ProjectPreviewResponseDTO.from(
                project,
                recruitmentMap.getOrDefault(project.getId(), List.of())
        )).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, projects.getTotalElements());
    }

    @Transactional
    public Map<RecruitmentCategory, Long> countProjectsPerCategory()
    {
        return projectRepository.countProjectsPerCategory();
    }

}
