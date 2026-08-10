package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.dto.chat.ChatMessageEvent;
import com.hicct3.projectfinder.dto.chat.ChatMessageResponseDTO;
import com.hicct3.projectfinder.dto.chat.ChatMessagesResponseDTO;
import com.hicct3.projectfinder.dto.chat.ChatRoomResponseDTO;
import com.hicct3.projectfinder.dto.chat.MarkChatReadRequestDTO;
import com.hicct3.projectfinder.dto.chat.SendChatMessageRequestDTO;
import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ProjectChatService;
import com.hicct3.projectfinder.websocket.ProjectChatWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectChatController {

    private final ProjectChatService projectChatService;
    private final ProjectChatWebSocketHandler webSocketHandler;

    @GetMapping("/chats")
    public ApiResponse<List<ChatRoomResponseDTO>> getChatRooms(Authentication authentication) {
        return ApiResponse.onSuccess(projectChatService.getChatRooms(userId(authentication)));
    }

    @GetMapping("/{projectId}/chat/messages")
    public ApiResponse<ChatMessagesResponseDTO> getMessages(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.onSuccess(projectChatService.getMessages(
                userId(authentication), projectId, beforeId, size));
    }

    @PostMapping("/{projectId}/chat/messages")
    public ApiResponse<ChatMessageResponseDTO> sendMessage(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody SendChatMessageRequestDTO request) {
        Long userId = userId(authentication);
        ChatMessageEvent event = projectChatService.sendMessage(userId, projectId, request.getContent());
        webSocketHandler.publish(event);
        return ApiResponse.onSuccess(event.forViewer(userId));
    }

    @PatchMapping("/{projectId}/chat/read")
    public ApiResponse<Void> markRead(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestBody(required = false) MarkChatReadRequestDTO request) {
        projectChatService.markRead(
                userId(authentication),
                projectId,
                request == null ? null : request.getLastMessageId());
        return ApiResponse.onSuccess(null);
    }

    private Long userId(Authentication authentication) {
        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}
