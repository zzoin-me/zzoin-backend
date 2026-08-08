package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.entity.enums.SortType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectQuestionRepository;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

        project.increaseViewCount();

        var recruitments = projectRecruitmentRepository.findByProject(project).stream().map(RecruitmentDetailResponseDTO::from).toList();
        var questions = projectQuestionRepository.findAllByProjectAndDeletedAtIsNullOrderByIdAsc(project).stream().map(QuestionResponseDTO::from).toList();

        return ProjectDetailResponseDTO.from(project, recruitments, questions);
    }

    @Transactional
    public Page<ProjectPreviewResponseDTO> getProjectList(SortType sort, String keyword, JobCategoryCode category, String name, Integer maxDays, Integer minCount, Integer maxCount, GoalType goal, Boolean recruitingOnly, Pageable pageable)
    {
        return projectRepository.searchProjects(sort, keyword, category, name, maxDays, minCount, maxCount, goal, recruitingOnly, pageable).map(
                project -> ProjectPreviewResponseDTO.from(project,
                        projectRecruitmentRepository.findByProject(project))
        );
    }

    @Transactional
    public Map<JobCategoryCode, Long> countProjectsPerCategory()
    {
        return projectRepository.countProjectsPerCategory();
    }

    @Transactional
    public Page<ProjectPreviewResponseDTO> getPopularProjects(Pageable pageable)
    {
        return projectRepository.findPopularProjects(pageable).map(
                project -> ProjectPreviewResponseDTO.from(project,
                        projectRecruitmentRepository.findByProject(project))
        );
    }

    @Transactional
    public Page<ProjectPreviewResponseDTO> getRecommendProjects(Long userId, Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        List<String> userFields = user.getFields();

        return projectRepository.findRecommendProjects(userId, userFields, pageable).map(
                project -> ProjectPreviewResponseDTO.from(project,
                        projectRecruitmentRepository.findByProject(project))
        );
    }

}
