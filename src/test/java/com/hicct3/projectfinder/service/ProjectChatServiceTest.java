package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.chat.ChatMessageEvent;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectChatMessage;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectChatMessageRepository;
import com.hicct3.projectfinder.repository.ProjectChatReadRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectChatServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectChatMessageRepository messageRepository;
    @Mock private ProjectChatReadRepository readRepository;
    @Mock private Clock clock;
    @Mock private SecurityRateLimitService rateLimitService;

    @InjectMocks
    private ProjectChatService projectChatService;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).nickName("팀원").build();
        project = Project.builder().id(10L).status(ProjectStatus.IN_PROGRESS).build();
    }

    @Test
    void memberCanSendTrimmedMessageWhileProjectIsInProgress() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-10T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        stubAccess(true);
        ProjectChatMessage savedMessage = mock(ProjectChatMessage.class);
        when(savedMessage.getId()).thenReturn(1L);
        when(savedMessage.getContent()).thenReturn("안녕하세요");
        when(savedMessage.getCreatedAt()).thenReturn(java.time.LocalDateTime.of(2026, 8, 10, 9, 0));
        when(messageRepository.save(any())).thenReturn(savedMessage);
        when(readRepository.findByProjectAndUser(project, user)).thenReturn(Optional.empty());

        ChatMessageEvent event = projectChatService.sendMessage(1L, 10L, "  안녕하세요  ");

        ArgumentCaptor<ProjectChatMessage> captor = ArgumentCaptor.forClass(ProjectChatMessage.class);
        verify(messageRepository).save(captor.capture());
        assertEquals("안녕하세요", captor.getValue().getContent());
        assertEquals("안녕하세요", event.content());
        verify(readRepository).save(any());
    }

    @Test
    void nonMemberCannotReadChat() {
        stubAccess(false);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectChatService.assertCanRead(1L, 10L));

        assertEquals(ErrorCode.USER_NOT_IN_PROJECT, exception.getErrorCode());
    }

    @Test
    void completedProjectChatIsReadOnly() {
        project.setStatus(ProjectStatus.COMPLETED);
        stubAccess(true);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> projectChatService.sendMessage(1L, 10L, "새 메시지"));

        assertEquals(ErrorCode.PROJECT_CHAT_READ_ONLY, exception.getErrorCode());
        verify(messageRepository, never()).save(any());
    }

    private void stubAccess(boolean member) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByUserAndProject(user, project)).thenReturn(member);
    }
}
