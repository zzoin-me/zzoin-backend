package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDeadlineServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private Clock clock;

    @InjectMocks
    private ProjectDeadlineService projectDeadlineService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(SEOUL);
    }

    @Test
    void closesRecruitmentAtTheExactDeadline() {
        User author = User.builder().userId(1L).build();
        Project project = Project.builder()
                .id(10L)
                .title("마감 테스트")
                .author(author)
                .status(ProjectStatus.RECRUITING)
                .recruitmentDeadline(LocalDateTime.ofInstant(NOW, SEOUL))
                .build();
        when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));
        when(notificationService.isAlreadyNotified(
                author, NotificationType.DEADLINE_REACHED, 10L)).thenReturn(false);

        assertTrue(projectDeadlineService.closeIfExpired(10L));
        assertEquals(ProjectStatus.RECRUITMENT_CLOSED, project.getStatus());
        verify(notificationService).createNotification(
                1L,
                NotificationType.DEADLINE_REACHED,
                "프로젝트 모집이 마감되었어요",
                "'마감 테스트' 프로젝트의 모집 기한이 종료되었습니다. 진행 상태를 변경해주세요.",
                "/projects/10/manage",
                10L);
    }

    @Test
    void keepsRecruitingBeforeTheDeadline() {
        Project project = Project.builder()
                .id(10L)
                .status(ProjectStatus.RECRUITING)
                .recruitmentDeadline(LocalDateTime.ofInstant(NOW.plusSeconds(1), SEOUL))
                .build();
        when(projectRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(project));

        assertFalse(projectDeadlineService.closeIfExpired(10L));
        assertEquals(ProjectStatus.RECRUITING, project.getStatus());
        verify(notificationService, never()).createNotification(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong());
    }
}
