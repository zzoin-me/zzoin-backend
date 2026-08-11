package com.hicct3.projectfinder.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hicct3.projectfinder.dto.chat.ChatMessageEvent;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.service.ProjectChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectChatWebSocketHandler extends TextWebSocketHandler {

    private final ProjectChatService projectChatService;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private final Map<Long, Set<WebSocketSession>> sessionsByProject = new ConcurrentHashMap<>();
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = attribute(session, ProjectChatHandshakeInterceptor.USER_ID);
        Long projectId = attribute(session, ProjectChatHandshakeInterceptor.PROJECT_ID);
        try {
            projectChatService.assertCanRead(userId, projectId);
            Set<WebSocketSession> userSessions = sessionsByUser.computeIfAbsent(
                    userId, ignored -> ConcurrentHashMap.newKeySet());
            synchronized (userSessions) {
                if (userSessions.size() >= 3) {
                    throw new GeneralException(ErrorCode.WEBSOCKET_CONNECTION_LIMIT);
                }
                userSessions.add(session);
            }
            sessionsByProject.computeIfAbsent(projectId, ignored -> ConcurrentHashMap.newKeySet())
                    .add(session);
            send(session, Map.of("type", "CONNECTED"));
        } catch (GeneralException exception) {
            sendError(session, exception);
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        Long userId = attribute(session, ProjectChatHandshakeInterceptor.USER_ID);
        Long projectId = attribute(session, ProjectChatHandshakeInterceptor.PROJECT_ID);
        try {
            IncomingMessage incoming = objectMapper.readValue(textMessage.getPayload(), IncomingMessage.class);
            ChatMessageEvent event = projectChatService.sendMessage(userId, projectId, incoming.content());
            publish(event);
        } catch (GeneralException exception) {
            sendError(session, exception);
        } catch (Exception exception) {
            send(session, Map.of(
                    "type", "ERROR",
                    "code", "INVALID_CHAT_MESSAGE",
                    "message", "메시지 형식이 올바르지 않습니다."));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = attribute(session, ProjectChatHandshakeInterceptor.USER_ID);
        Long projectId = attribute(session, ProjectChatHandshakeInterceptor.PROJECT_ID);
        removeSession(sessionsByUser, userId, session);
        removeSession(sessionsByProject, projectId, session);
    }

    private void removeSession(
            Map<Long, Set<WebSocketSession>> sessionsByKey,
            Long key,
            WebSocketSession session) {
        if (key == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByKey.get(key);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByKey.remove(key, sessions);
        }
    }

    public void publish(ChatMessageEvent event) {
        Set<WebSocketSession> sessions = sessionsByProject.getOrDefault(event.projectId(), Set.of());
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            Long viewerId = attribute(session, ProjectChatHandshakeInterceptor.USER_ID);
            send(session, Map.of(
                    "type", "MESSAGE",
                    "message", event.forViewer(viewerId)));
        }
    }

    private void sendError(WebSocketSession session, GeneralException exception) {
        send(session, Map.of(
                "type", "ERROR",
                "code", exception.getErrorCode().getCode(),
                "message", exception.getMessage()));
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                }
            }
        } catch (IOException exception) {
            log.warn("Failed to send project chat WebSocket message", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T attribute(WebSocketSession session, String key) {
        return (T) session.getAttributes().get(key);
    }

    private record IncomingMessage(String content) {
    }
}
