package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.*;
import com.hicct3.projectfinder.dto.project.myproject.MyApplicationPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyProjectPreviewResponseDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.*;
import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final JobRoleRepository jobRoleRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;
    private final ProjectQuestionRepository projectQuestionRepository;
    private final RecruitmentService recruitmentService;
    private final ProjectDeadlineScheduler projectDeadlineScheduler;
    private final Clock clock;

    @Transactional
    public Page<MyProjectPreviewResponseDTO> getMyProjects(
            Long userId,
            String status,
            boolean hasApplicants,
            Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        String normalizedStatus = "RECRUITING".equals(status) || "CLOSED".equals(status)
                ? status
                : null;

        return projectRepository.findMyProjects(user, normalizedStatus, hasApplicants, pageable)
                .map(project -> MyProjectPreviewResponseDTO.from(
                        project,
                        projectRecruitmentRepository.findAllByProjectAndDeletedAtIsNull(project)
                ));
    }

    @Transactional
    public Page<MyApplicationPreviewResponseDTO> getMyApplications(Long userId, ApplicationStatus status, Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var applications = status != null
                ? projectApplicationRepository.findAllByUserAndStatus(user, status, pageable)
                : projectApplicationRepository.findAllByUser(user, pageable);

        return applications.map(MyApplicationPreviewResponseDTO::from);
    }

    //프로젝트 생성
    @Transactional
    public void createProject(Long userId, CreateProjectRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        if (req.getRecruitments() == null || req.getRecruitments().isEmpty()) {
            throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);
        }

        //프로젝트 엔티티 생성
        Project savedProject = projectRepository.save(Project.create(req, user, clock));

        //직군 저장
        recruitmentService.createRecruitments(savedProject, req.getRecruitments());

        createQuestions(savedProject, req.getQuestions());

        //생성자를 멤버로 할당
        var member = ProjectMember.builder()
                .status(MemberStatus.ACTIVE)
                .user(user)
                .project(savedProject)
                .role(Role.OWNER)
                .joinedAt(LocalDateTime.now(clock))
                .build();

        projectMemberRepository.save(member);
        projectDeadlineScheduler.scheduleAfterCommit(
                savedProject.getId(),
                savedProject.getRecruitmentDeadline());

    }

    private void createQuestions(Project project, List<CreateQuestionRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        List<ProjectQuestion> questions = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateQuestionRequestDTO request = requests.get(i);
            List<String> options = request.getOptions() == null
                    ? Collections.emptyList()
                    : request.getOptions().stream()
                            .map(String::trim)
                            .filter(option -> !option.isEmpty())
                            .toList();

            if (request.getType() != QuestionType.TEXT && options.size() < 2) {
                throw new GeneralException("선택형 질문은 선택지를 2개 이상 등록해야 합니다.");
            }

            questions.add(ProjectQuestion.builder()
                    .project(project)
                    .orderIndex(i)
                    .type(request.getType())
                    .label(request.getLabel().trim())
                    .options(request.getType() == QuestionType.TEXT ? null : String.join(",", options))
                    .required(request.getRequired())
                    .createdAt(LocalDateTime.now(clock))
                    .build());
        }

        projectQuestionRepository.saveAll(questions);
    }



    //프로젝트 수정
    @Transactional
    public void updateProject(Long userId, Long projectId, UpdateProjectRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        if(project.getStatus() != ProjectStatus.RECRUITING)
            throw new GeneralException(ErrorCode.PROJECT_EDIT_CLOSED);

        project.setUpdatedAt(LocalDateTime.now(clock));

        if(req.getTitle() != null)
            project.setTitle(req.getTitle());

        if(req.getDescription() != null)
            project.setDescription(req.getDescription());

        if(req.getCollaborationType() != null)
            project.setCollaborationType(req.getCollaborationType());

        if(req.getCommunicationTool() != null)
            project.setCommunicationTool(req.getCommunicationTool());

        if(req.getMeetingSchedule() != null)
            project.setMeetingSchedule(req.getMeetingSchedule());

        if(req.getPeriod() != null)
            project.setPeriod(req.getPeriod());

        if(req.getRecruitmentDeadline() != null) {
            project.setRecruitmentDeadline(req.getRecruitmentDeadline());
            if (project.getStatus() == ProjectStatus.RECRUITING) {
                projectDeadlineScheduler.scheduleAfterCommit(
                        project.getId(),
                        req.getRecruitmentDeadline());
            }
        }

        if(req.getGoalType() != null)
            project.setGoal(req.getGoalType());

        if(req.getImageUrl() != null)
            project.setImageUrl(req.getImageUrl());

        if(req.getRecruitments() != null)
        {
            recruitmentService.updateRecruitments(project, req.getRecruitments());
        }
    }


    //프로젝트 제거
    @Transactional
    public void deleteProject(Long userId, Long projectId)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
            throw new GeneralException(ErrorCode.PROJECT_ALREADY_DELETED);

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        if(projectApplicationRepository.existsByProject(project))
            throw new GeneralException(ErrorCode.PROJECT_HAS_APPLICATIONS);

       project.setDeletedAt(LocalDateTime.now(clock));
       projectDeadlineScheduler.cancelAfterCommit(project.getId());
    }

    //프로젝트 상태 변경
    @Transactional
    public void setProjectStatus(Long userId, Long projectId, UpdateProjectStatusRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getStatus().equals(ProjectStatus.COMPLETED))
            throw new GeneralException(ErrorCode.PROJECT_ALREADY_COMPLETED);

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        validateStatusTransition(project.getStatus(), req.getStatus());

        if (req.getStatus() == ProjectStatus.IN_PROGRESS
                && projectApplicationRepository.existsByProjectAndStatus(
                        project, ApplicationStatus.PENDING)) {
            throw new GeneralException(ErrorCode.PENDING_APPLICATIONS_EXIST);
        }

        if(req.getStatus().equals(ProjectStatus.COMPLETED))
        {
            for (ProjectMember projectMember : projectMemberRepository.findAllByProject(project)) {
                projectMember.setCompletedAt(LocalDateTime.now(clock));
                projectMember.setStatus(MemberStatus.COMPLETED);
            }
        }

        project.setStatus(req.getStatus());
        project.setUpdatedAt(LocalDateTime.now(clock));
        if (req.getStatus() == ProjectStatus.RECRUITING) {
            projectDeadlineScheduler.scheduleAfterCommit(
                    project.getId(),
                    project.getRecruitmentDeadline());
        } else {
            projectDeadlineScheduler.cancelAfterCommit(project.getId());
        }
    }

    private void validateStatusTransition(ProjectStatus current, ProjectStatus next) {
        boolean valid = (current == ProjectStatus.RECRUITING
                && next == ProjectStatus.RECRUITMENT_CLOSED)
                || (current == ProjectStatus.RECRUITMENT_CLOSED
                && next == ProjectStatus.IN_PROGRESS)
                || (current == ProjectStatus.IN_PROGRESS
                && next == ProjectStatus.COMPLETED);
        if (!valid) {
            throw new GeneralException(ErrorCode.INVALID_PROJECT_STATUS_TRANSITION);
        }
    }

}
