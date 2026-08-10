package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.project.UpdateProjectStatusRequestDTO;
import com.hicct3.projectfinder.dto.project.UpdateProjectRequestDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.JobCategoryRepository;
import com.hicct3.projectfinder.repository.JobRoleRepository;
import com.hicct3.projectfinder.repository.MemberReviewRepository;
import com.hicct3.projectfinder.repository.ProjectApplicationRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectQuestionRepository;
import com.hicct3.projectfinder.repository.ProjectRecruitmentRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceStatusTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectRecruitmentRepository projectRecruitmentRepository;
    @Mock private ProjectApplicationRepository projectApplicationRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private MemberReviewRepository memberReviewRepository;
    @Mock private JobRoleRepository jobRoleRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectQuestionRepository projectQuestionRepository;
    @Mock private RecruitmentService recruitmentService;
    @Mock private ProjectDeadlineScheduler projectDeadlineScheduler;
    @Mock private Clock clock;

    @InjectMocks
    private ProjectService projectService;

    private User author;

    @BeforeEach
    void setUp() {
        author = User.builder().userId(1L).build();
    }

    @Test
    void startsOnlyAfterAllPendingApplicationsAreProcessed() {
        Project project = project(ProjectStatus.RECRUITMENT_CLOSED);
        stubProject(project);
        when(projectApplicationRepository.existsByProjectAndStatus(project, ApplicationStatus.PENDING))
                .thenReturn(true);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectService.setProjectStatus(
                        1L,
                        10L,
                        UpdateProjectStatusRequestDTO.builder()
                                .status(ProjectStatus.IN_PROGRESS)
                                .build()));

        assertEquals(ErrorCode.PENDING_APPLICATIONS_EXIST, exception.getErrorCode());
        assertEquals(ProjectStatus.RECRUITMENT_CLOSED, project.getStatus());
    }

    @Test
    void rejectsSkippingLifecycleSteps() {
        Project project = project(ProjectStatus.RECRUITING);
        stubProject(project);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectService.setProjectStatus(
                        1L,
                        10L,
                        UpdateProjectStatusRequestDTO.builder()
                                .status(ProjectStatus.COMPLETED)
                                .build()));

        assertEquals(ErrorCode.INVALID_PROJECT_STATUS_TRANSITION, exception.getErrorCode());
        verify(projectDeadlineScheduler, never()).cancelAfterCommit(10L);
    }

    @Test
    void completingProjectCompletesAllMembers() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-10T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        Project project = project(ProjectStatus.IN_PROGRESS);
        ProjectMember member = ProjectMember.builder().status(MemberStatus.ACTIVE).build();
        stubProject(project);
        when(projectMemberRepository.findAllByProject(project)).thenReturn(java.util.List.of(member));

        projectService.setProjectStatus(
                1L,
                10L,
                UpdateProjectStatusRequestDTO.builder().status(ProjectStatus.COMPLETED).build());

        assertEquals(ProjectStatus.COMPLETED, project.getStatus());
        assertEquals(MemberStatus.COMPLETED, member.getStatus());
        assertEquals(java.time.LocalDateTime.of(2026, 8, 10, 9, 0), member.getCompletedAt());
        verify(projectDeadlineScheduler).cancelAfterCommit(10L);
    }

    @Test
    void rejectsEditingAfterRecruitmentIsClosed() {
        Project project = project(ProjectStatus.RECRUITMENT_CLOSED);
        project.setTitle("기존 제목");
        stubProject(project);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectService.updateProject(
                        1L,
                        10L,
                        UpdateProjectRequestDTO.builder().title("바뀐 제목").build()));

        assertEquals(ErrorCode.PROJECT_EDIT_CLOSED, exception.getErrorCode());
        assertEquals("기존 제목", project.getTitle());
    }

    private Project project(ProjectStatus status) {
        return Project.builder().id(10L).author(author).status(status).build();
    }

    private void stubProject(Project project) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
    }
}
