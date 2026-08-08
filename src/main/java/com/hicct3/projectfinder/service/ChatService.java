package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.global.JwtProvider;
import com.hicct3.projectfinder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    private final Map<Long, List<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    @Transactional
    public ChatRoom createChatRoomForProject(Project project) {
        return chatRoomRepository.findByProject(project).orElseGet(() -> {
            ChatRoom room = ChatRoom.builder()
                    .project(project)
                    .createdAt(LocalDateTime.now())
                    .build();
            room = chatRoomRepository.save(room);

            ChatMessage systemMsg = ChatMessage.builder()
                    .chatRoom(room)
                    .content("프로젝트가 시작되었어요! 팀원들과 소통해보세요.")
                    .isSystem(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            chatMessageRepository.save(systemMsg);

            return room;
        });
    }

    @Transactional
    public Map<String, Object> getChatRoom(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findByProject(project)
                .orElseThrow(() -> new GeneralException(ErrorCode.COMMON_BAD_REQUEST));

        checkAccess(userId, project);

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(
                room, PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "createdAt")));

        List<Map<String, Object>> msgList = messages.getContent().stream()
                .map(this::toMessageMap)
                .toList();

        return Map.of(
                "roomId", room.getId(),
                "projectId", projectId,
                "projectTitle", project.getTitle(),
                "messages", msgList
        );
    }

    @Transactional
    public Map<String, Object> sendMessage(Long userId, Long projectId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));

        checkAccess(userId, project);

        ChatRoom room = chatRoomRepository.findByProject(project)
                .orElseThrow(() -> new GeneralException(ErrorCode.COMMON_BAD_REQUEST));

        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .user(user)
                .content(content)
                .isSystem(false)
                .createdAt(LocalDateTime.now())
                .build();

        msg = chatMessageRepository.save(msg);

        Map<String, Object> msgMap = toMessageMap(msg);
        broadcastMessage(room.getId(), msgMap);

        return msgMap;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMessages(Long userId, Long projectId, int page, int size) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
        checkAccess(userId, project);

        ChatRoom room = chatRoomRepository.findByProject(project)
                .orElseThrow(() -> new GeneralException(ErrorCode.COMMON_BAD_REQUEST));

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByCreatedAtDesc(
                room, PageRequest.of(page, size));

        return messages.map(this::toMessageMap);
    }

    public SseEmitter subscribe(Long projectId, String token) {
        Long userId = getUserIdFromToken(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorCode.PROJECT_NOT_FOUND));
        checkAccess(userId, project);

        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L);

        roomEmitters.computeIfAbsent(projectId, k -> new ArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(projectId, emitter));
        emitter.onTimeout(() -> removeEmitter(projectId, emitter));
        emitter.onError(e -> removeEmitter(projectId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            removeEmitter(projectId, emitter);
        }

        return emitter;
    }

    private void broadcastMessage(Long roomId, Map<String, Object> msg) {
        List<SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name("message").data(msg));
            } catch (Exception ex) {
                dead.add(e);
            }
        }
        emitters.removeAll(dead);
    }

    private void removeEmitter(Long projectId, SseEmitter emitter) {
        List<SseEmitter> emitters = roomEmitters.get(projectId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    private void checkAccess(Long userId, Project project) {
        if (project.getAuthor().getUserId().equals(userId)) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        boolean isMember = projectMemberRepository.findAllByProject(project).stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId)
                        && (m.getStatus() == MemberStatus.IN_PROGRESS || m.getStatus() == MemberStatus.COMPLETED));

        if (!isMember) {
            throw new GeneralException(ErrorCode.COMMON_BAD_REQUEST);
        }
    }

    private Map<String, Object> toMessageMap(ChatMessage msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", msg.getId());
        m.put("content", msg.getContent());
        m.put("isSystem", msg.getIsSystem());
        m.put("createdAt", msg.getCreatedAt().toString());
        if (msg.getUser() != null) {
            m.put("userId", msg.getUser().getUserId());
            m.put("nickname", msg.getUser().getNickName());
            m.put("profileUrl", msg.getUser().getProfileUrl());
        }
        return m;
    }

    private Long getUserIdFromToken(String token) {
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        return jwtProvider.verifyAccessTokenAndGetUserId(cleanToken);
    }
}
