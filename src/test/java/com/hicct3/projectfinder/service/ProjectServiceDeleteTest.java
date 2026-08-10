package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.User;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceDeleteTest {

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

    @Test
    void cannotDeleteProjectWhenAnyApplicationExists() {
        User author = User.builder().userId(1L).build();
        Project project = Project.builder().id(10L).author(author).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectApplicationRepository.existsByProject(project)).thenReturn(true);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectService.deleteProject(1L, 10L));

        assertEquals(ErrorCode.PROJECT_HAS_APPLICATIONS, exception.getErrorCode());
        assertNull(project.getDeletedAt());
        verify(projectDeadlineScheduler, never()).cancelAfterCommit(10L);
    }
}
