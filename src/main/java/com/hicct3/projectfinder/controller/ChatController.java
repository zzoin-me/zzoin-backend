package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/chatroom")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "대화방 정보 + 최근 메시지")
    @GetMapping
    public ApiResponse<Map<String, Object>> getChatRoom(
            Authentication authentication,
            @PathVariable Long projectId) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(chatService.getChatRoom(userDetails.getId(), projectId));
    }

    @Operation(summary = "과거 메시지 페이지네이션")
    @GetMapping("/messages")
    public ApiResponse<Page<Map<String, Object>>> getMessages(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ApiResponse.onSuccess(chatService.getMessages(userDetails.getId(), projectId, page, size));
    }

    @Operation(summary = "메시지 전송")
    @PostMapping("/messages")
    public ApiResponse<Map<String, Object>> sendMessage(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> result = chatService.sendMessage(
                userDetails.getId(), projectId, body.get("content"));
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "채팅 SSE 스트림")
    @GetMapping("/stream")
    public SseEmitter stream(@PathVariable Long projectId, @RequestParam String token) {
        return chatService.subscribe(projectId, token);
    }
}
