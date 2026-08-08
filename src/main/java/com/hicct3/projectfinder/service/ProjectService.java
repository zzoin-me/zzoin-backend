package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectStatusRequestDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyApplicationPreviewResponseDTO;
import com.hicct3.projectfinder.dto.project.myproject.MyProjectPreviewResponseDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.ProjectQuestion;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import com.hicct3.projectfinder.entity.enums.RecruitmentRole;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectApplicationRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectQuestionRepository;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectRecruitmentRepository projectRecruitmentRepository;
    private final ProjectApplicationRepository projectApplicationRepository;
    private final ProjectQuestionRepository projectQuestionRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;

    @Transactional(readOnly = true)
    public Page<MyProjectPreviewResponseDTO> getMyProjects(Long userId, String statusFilter, Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Page<Project> projects;
        if ("RECRUITING".equals(statusFilter)) {
            projects = projectRepository.findAllByAuthorAndDeletedAtIsNullAndStatusOrderByIdDesc(user, ProjectStatus.RECRUITING, pageable);
        } else if ("CLOSED".equals(statusFilter)) {
            projects = projectRepository.findAllByAuthorAndDeletedAtIsNullAndStatusNotOrderByIdDesc(user, ProjectStatus.RECRUITING, pageable);
        } else {
            projects = projectRepository.findAllByAuthorAndDeletedAtIsNullOrderByIdDesc(user, pageable);
        }

        List<MyProjectPreviewResponseDTO> dtos = projects.getContent().stream()
                .map(project -> MyProjectPreviewResponseDTO.from(
                        project,
                        projectRecruitmentRepository.findAllByProjectAndDeletedAtIsNull(project)
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, projects.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<MyApplicationPreviewResponseDTO> getMyApplications(Long userId, ApplicationStatus status, Pageable pageable)
    {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var applications = status != null
                ? projectApplicationRepository.findAllByUserAndStatus(user, status, pageable)
                : projectApplicationRepository.findAllByUser(user, pageable);

        List<MyApplicationPreviewResponseDTO> dtos = applications.getContent().stream()
                .map(MyApplicationPreviewResponseDTO::from)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, applications.getTotalElements());
    }

    @Transactional
    public void createProject(Long userId, CreateProjectRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));

        if(req.getRecruitments().isEmpty())
            throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);

        var project = Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .collaborationType(req.getCollaborationType())
                .communicationTool(req.getCommunicationTool())
                .meetingSchedule(req.getMeetingSchedule())
                .period(req.getPeriod())
                .recruitmentDeadline(req.getRecruitmentDeadline())
                .goal(req.getGoalType())
                .imageUrl(req.getImageUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .author(user)
                .status(ProjectStatus.RECRUITING)
                .build();

        var savedProject = projectRepository.save(project);

        req.getRecruitments().forEach(x->
        {
            validateRecruitmentRole(x.getCategory(), x.getName());
            projectRecruitmentRepository.save(ProjectRecruitment.builder()
                    .name(x.getName())
                    .category(x.getCategory())
                    .recruitmentCount(x.getCount())
                    .qualification(x.getQualification())
                    .preferred(x.getPreferred())
                    .applicantCount(0)
                    .project(savedProject)
                    .build());
        });

        saveQuestions(savedProject, req.getQuestions());

    }

    private void saveQuestions(Project project, java.util.List<com.hicct3.projectfinder.dto.project.CreateQuestionRequestDTO> questions) {
        if (questions == null || questions.isEmpty()) return;
        if (questions.size() > 10) {
            throw new GeneralException(ErrorCode.QUESTION_LIMIT_EXCEEDED);
        }
        for (int i = 0; i < questions.size(); i++) {
            var q = questions.get(i);
            projectQuestionRepository.save(ProjectQuestion.builder()
                    .project(project)
                    .orderIndex(i)
                    .type(q.getType())
                    .label(q.getLabel())
                    .options(q.getOptions() != null && !q.getOptions().isEmpty() ? String.join(",", q.getOptions()) : null)
                    .required(q.getRequired())
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

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

        project.setUpdatedAt(LocalDateTime.now());

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

        if(req.getRecruitmentDeadline() != null)
            project.setRecruitmentDeadline(req.getRecruitmentDeadline());

        if(req.getGoalType() != null)
            project.setGoal(req.getGoalType());

        if(req.getImageUrl() != null)
            project.setImageUrl(req.getImageUrl());

        if(req.getRecruitments() != null)
        {
            if(req.getRecruitments().isEmpty())
                throw new GeneralException(ErrorCode.RECRUITMENT_EMPTY);

            List<ProjectRecruitment> existingRecruitments =
                    projectRecruitmentRepository.findAllByProjectAndDeletedAtIsNull(project);

            Set<Long> requestedRecruitmentIds = new HashSet<>();

            for (var x : req.getRecruitments()) {
                validateRecruitmentRole(x.getCategory(), x.getName());
                if (x.getRecruitmentId() == null) {
                    ProjectRecruitment newRecruitment = ProjectRecruitment.builder()
                            .name(x.getName())
                            .category(x.getCategory())
                            .applicantCount(0)
                            .recruitmentCount(x.getCount())
                            .qualification(x.getQualification())
                            .preferred(x.getPreferred())
                            .project(project)
                            .build();

                    projectRecruitmentRepository.save(newRecruitment);
                    continue;
                }

                ProjectRecruitment recruitment = existingRecruitments.stream()
                        .filter(r -> r.getId().equals(x.getRecruitmentId()))
                        .findFirst()
                        .orElseThrow(() -> new GeneralException(ErrorCode.RECRUITMENT_NOT_FOUND));

                requestedRecruitmentIds.add(recruitment.getId());

                recruitment.setName(x.getName());
                recruitment.setCategory(x.getCategory());
                recruitment.setRecruitmentCount(x.getCount());
                recruitment.setQualification(x.getQualification());
                recruitment.setPreferred(x.getPreferred());
            }

            existingRecruitments.stream()
                    .filter(recruitment -> !requestedRecruitmentIds.contains(recruitment.getId()))
                    .forEach(recruitment -> recruitment.setDeletedAt(LocalDateTime.now()));
        }

        if(req.getQuestions() != null) {
            projectQuestionRepository.findAllByProjectInAndDeletedAtIsNull(List.of(project))
                    .forEach(q -> q.setDeletedAt(LocalDateTime.now()));
            saveQuestions(project, req.getQuestions());
        }
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
            throw new GeneralException(ErrorCode.PROJECT_ALREADY_DELETED);

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

       project.setDeletedAt(LocalDateTime.now());
    }

    @Transactional
    public void setProjectStatus(Long userId, Long projectId, UpdateProjectStatusRequestDTO req)
    {
        var user = userRepository.findById(userId).orElseThrow(()->new GeneralException(ErrorCode.USER_NOT_FOUND));
        var project = projectRepository.findById(projectId).orElseThrow(()->new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        if(project.getDeletedAt() != null)
        {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }

        if(!project.getAuthor().getUserId().equals(user.getUserId()))
            throw new GeneralException(ErrorCode.AUTHOR_MISMATCHED);

        ProjectStatus current = project.getStatus();
        ProjectStatus next = req.getStatus();

        validateStatusTransition(current, next);

        if (next == ProjectStatus.IN_PROGRESS) {
            project.setStatus(ProjectStatus.IN_PROGRESS);
            chatService.createChatRoomForProject(project);
            notifyMembersStarted(project);
        } else if (next == ProjectStatus.COMPLETED) {
            project.setStatus(ProjectStatus.COMPLETED);
            completeAllMembers(project);
        } else {
            throw new GeneralException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void validateStatusTransition(ProjectStatus current, ProjectStatus next) {
        boolean valid = switch (current) {
            case RECRUITMENT_CLOSED -> next == ProjectStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == ProjectStatus.COMPLETED;
            default -> false;
        };
        if (!valid) {
            throw new GeneralException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void completeAllMembers(Project project) {
        List<ProjectMember> members = projectMemberRepository.findAllByProject(project);
        LocalDateTime now = LocalDateTime.now();
        for (ProjectMember m : members) {
            if (m.getStatus() == MemberStatus.IN_PROGRESS) {
                m.setStatus(MemberStatus.COMPLETED);
                m.setCompletedAt(now);
                notificationService.createNotification(
                        m.getUser().getUserId(),
                        NotificationType.APPLICATION_APPROVED,
                        "프로젝트가 완료되었어요 🎉",
                        "'" + project.getTitle() + "' 프로젝트가 완료되었습니다. 팀원 후기를 작성해주세요.",
                        "/mypage/reviews",
                        project.getId());
            }
        }
    }

    private void notifyMembersStarted(Project project) {
        List<ProjectMember> members = projectMemberRepository.findAllByProject(project);
        for (ProjectMember m : members) {
            notificationService.createNotification(
                    m.getUser().getUserId(),
                    NotificationType.APPLICATION_APPROVED,
                    "프로젝트가 시작되었어요! 🚀",
                    "'" + project.getTitle() + "' 프로젝트가 진행을 시작했습니다. 대화방에서 팀원들과 소통해보세요!",
                    "/projects/" + project.getId(),
                    project.getId());
        }
    }

    private void validateRecruitmentRole(RecruitmentCategory category, String name) {
        if (category == null) {
            return;
        }
        if (name == null || name.isBlank()) {
            throw new GeneralException(ErrorCode.INVALID_RECRUITMENT_ROLE);
        }
        var allowed = RecruitmentRole.displayNamesByCategory(category);
        if (!allowed.contains(name)) {
            throw new GeneralException(ErrorCode.INVALID_RECRUITMENT_ROLE);
        }
    }
}

