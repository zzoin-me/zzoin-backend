package com.hicct3.projectfinder.controller;

import com.hicct3.projectfinder.global.ApiResponse;
import com.hicct3.projectfinder.entity.Notification;
import com.hicct3.projectfinder.global.CustomUserDetails;
import com.hicct3.projectfinder.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ApiResponse<Map<String, Object>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Page<Notification> notifications = notificationService.getNotifications(
                userDetails.getId(), PageRequest.of(page, size));

        List<Map<String, Object>> content = notifications.getContent().stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "type", n.getType().name(),
                        "title", n.getTitle(),
                        "content", n.getContent() != null ? n.getContent() : "",
                        "targetUrl", n.getTargetUrl() != null ? n.getTargetUrl() : "",
                        "isRead", n.getIsRead(),
                        "createdAt", n.getCreatedAt().toString()
                ))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", notifications.getTotalElements());
        result.put("totalPages", notifications.getTotalPages());
        result.put("currentPage", notifications.getNumber());
        return ApiResponse.onSuccess(result);
    }

    @Operation(summary = "읽지 않은 알림 개수")
    @GetMapping("/unread")
    public ApiResponse<Map<String, Long>> getUnreadCount(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        long count = notificationService.getUnreadCount(userDetails.getId());
        return ApiResponse.onSuccess(Map.of("count", count));
    }

    @Operation(summary = "SSE 알림 스트림")
    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token) {
        Long userId = notificationService.getUserIdFromToken(token);
        return notificationService.subscribe(userId);
    }

    @Operation(summary = "개별 알림 읽음 처리")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "전체 알림 읽음 처리")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        notificationService.markAllAsRead(userDetails.getId());
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "FCM 디바이스 토큰 등록")
    @PostMapping("/device")
    public ApiResponse<Void> registerDevice(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        notificationService.registerDeviceToken(
                userDetails.getId(),
                body.get("token"),
                body.getOrDefault("platform", "ANDROID"));
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "FCM 디바이스 토큰 해제")
    @DeleteMapping("/device")
    public ApiResponse<Void> unregisterDevice(@RequestBody Map<String, String> body) {
        notificationService.unregisterDeviceToken(body.get("token"));
        return ApiResponse.onSuccess(null);
    }
}
