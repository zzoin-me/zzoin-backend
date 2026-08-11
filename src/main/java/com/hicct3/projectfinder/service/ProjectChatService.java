package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.chat.ChatMessageEvent;
import com.hicct3.projectfinder.dto.chat.ChatMessageResponseDTO;
import com.hicct3.projectfinder.dto.chat.ChatMessagesResponseDTO;
import com.hicct3.projectfinder.dto.chat.ChatRoomResponseDTO;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectChatMessage;
import com.hicct3.projectfinder.entity.ProjectChatRead;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.ProjectChatMessageRepository;
import com.hicct3.projectfinder.repository.ProjectChatReadRepository;
import com.hicct3.projectfinder.repository.ProjectMemberRepository;
import com.hicct3.projectfinder.repository.ProjectRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectChatService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectChatMessageRepository messageRepository;
    private final ProjectChatReadRepository readRepository;
    private final Clock clock;
    private final SecurityRateLimitService rateLimitService;

    @Transactional(readOnly = true)
    public void assertCanRead(Long userId, Long projectId) {
        AccessContext context = getAccessContext(userId, projectId);
        ensureChatAvailable(context.project());
    }

    @Transactional(readOnly = true)
    public ChatMessagesResponseDTO getMessages(
            Long userId,
            Long projectId,
            Long beforeId,
            int requestedSize) {
        AccessContext context = getAccessContext(userId, projectId);
        ensureChatAvailable(context.project());

        int size = Math.max(1, Math.min(requestedSize, MAX_PAGE_SIZE));
        PageRequest pageable = PageRequest.of(0, size);
        Page<ProjectChatMessage> page = beforeId == null
                ? messageRepository.findAllByProjectOrderByIdDesc(context.project(), pageable)
                : messageRepository.findAllByProjectAndIdLessThanOrderByIdDesc(
                        context.project(), beforeId, pageable);

        List<ChatMessageResponseDTO> messages = new ArrayList<>(page.getContent().stream()
                .map(message -> ChatMessageResponseDTO.from(message, userId))
                .toList());
        Collections.reverse(messages);

        return ChatMessagesResponseDTO.builder()
                .messages(messages)
                .nextCursor(page.hasNext() && !messages.isEmpty() ? messages.get(0).getId() : null)
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponseDTO> getChatRooms(Long userId) {
        User user = findUser(userId);
        List<ProjectMember> memberships = projectMemberRepository
                .findAllByUserAndProject_StatusInOrderByProject_UpdatedAtDesc(
                        user,
                        List.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED));

        return memberships.stream().map(member -> {
            Project project = member.getProject();
            ProjectChatMessage lastMessage = messageRepository
                    .findTopByProjectOrderByIdDesc(project)
                    .orElse(null);
            long lastReadId = readRepository.findByProjectAndUser(project, user)
                    .map(ProjectChatRead::getLastReadMessageId)
                    .orElse(0L);
            return ChatRoomResponseDTO.builder()
                    .projectId(project.getId())
                    .projectTitle(project.getTitle())
                    .projectImageUrl(project.getImageUrl())
                    .projectStatus(project.getStatus())
                    .lastMessage(lastMessage == null ? null : lastMessage.getContent())
                    .lastMessageAt(lastMessage == null ? null : lastMessage.getCreatedAt())
                    .unreadCount(messageRepository.countByProjectAndIdGreaterThan(project, lastReadId))
                    .build();
        }).sorted(Comparator.comparing(
                ChatRoomResponseDTO::getLastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public ChatMessageEvent sendMessage(Long userId, Long projectId, String rawContent) {
        rateLimitService.consume(
                "chat-message", userId + "|" + projectId, 5, Duration.ofSeconds(1));
        AccessContext context = getAccessContext(userId, projectId);
        ensureChatAvailable(context.project());
        if (context.project().getStatus() == ProjectStatus.COMPLETED) {
            throw new GeneralException(ErrorCode.PROJECT_CHAT_READ_ONLY);
        }

        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 1000) {
            throw new GeneralException(ErrorCode.INVALID_CHAT_MESSAGE);
        }

        ProjectChatMessage message = messageRepository.save(ProjectChatMessage.builder()
                .project(context.project())
                .sender(context.user())
                .content(content)
                .createdAt(LocalDateTime.now(clock))
                .build());

        markReadInternal(context.project(), context.user(), message.getId());
        return new ChatMessageEvent(
                message.getId(),
                context.project().getId(),
                context.user().getUserId(),
                context.user().getNickName(),
                context.user().getProfileUrl(),
                message.getContent(),
                message.getCreatedAt());
    }

    @Transactional
    public void markRead(Long userId, Long projectId, Long requestedMessageId) {
        AccessContext context = getAccessContext(userId, projectId);
        ensureChatAvailable(context.project());

        Long messageId = requestedMessageId;
        if (messageId == null) {
            messageId = messageRepository.findTopByProjectOrderByIdDesc(context.project())
                    .map(ProjectChatMessage::getId)
                    .orElse(0L);
        } else if (messageRepository.findByIdAndProject(messageId, context.project()).isEmpty()) {
            throw new GeneralException(ErrorCode.INVALID_CHAT_MESSAGE);
        }
        markReadInternal(context.project(), context.user(), messageId);
    }

    private void markReadInternal(Project project, User user, Long messageId) {
        ProjectChatRead read = readRepository.findByProjectAndUser(project, user)
                .orElseGet(() -> ProjectChatRead.builder()
                        .project(project)
                        .user(user)
                        .lastReadMessageId(0L)
                        .updatedAt(LocalDateTime.now(clock))
                        .build());
        if (messageId > read.getLastReadMessageId()) {
            read.setLastReadMessageId(messageId);
        }
        read.setUpdatedAt(LocalDateTime.now(clock));
        readRepository.save(read);
    }

    private AccessContext getAccessContext(Long userId, Long projectId) {
        User user = findUser(userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
        if (project.getDeletedAt() != null) {
            throw new GeneralException(ErrorCode.PROJECT_DELETED);
        }
        if (!projectMemberRepository.existsByUserAndProject(user, project)) {
            throw new GeneralException(ErrorCode.USER_NOT_IN_PROJECT);
        }
        return new AccessContext(project, user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
    }

    private void ensureChatAvailable(Project project) {
        if (project.getStatus() != ProjectStatus.IN_PROGRESS
                && project.getStatus() != ProjectStatus.COMPLETED) {
            throw new GeneralException(ErrorCode.PROJECT_CHAT_NOT_AVAILABLE);
        }
    }

    private record AccessContext(Project project, User user) {
    }
}
