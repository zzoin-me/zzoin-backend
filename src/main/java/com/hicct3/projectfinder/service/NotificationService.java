package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.DeviceToken;
import com.hicct3.projectfinder.entity.Notification;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.DeviceTokenRepository;
import com.hicct3.projectfinder.repository.NotificationRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import com.hicct3.projectfinder.global.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;
    private final JwtProvider jwtProvider;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Transactional
    public void createNotification(Long userId, NotificationType type, String title,
                                   String content, String targetUrl, Long refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .targetUrl(targetUrl)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .refId(refId)
                .build();

        notificationRepository.save(notification);

        sendSse(userId, notification);
        fcmPushService.sendToUser(userId, title, content, targetUrl);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GeneralException(ErrorCode.COMMON_BAD_REQUEST));
        n.setIsRead(true);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        Page<Notification> unread = notificationRepository.findByUserOrderByCreatedAtDesc(user, Pageable.ofSize(100));
        unread.getContent().forEach(n -> {
            if (!n.getIsRead()) n.setIsRead(true);
        });
    }

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(60L * 60L * 1000L);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (Exception e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    private void sendSse(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(Map.of(
                                "id", notification.getId(),
                                "type", notification.getType(),
                                "title", notification.getTitle(),
                                "content", notification.getContent(),
                                "targetUrl", notification.getTargetUrl() != null ? notification.getTargetUrl() : "",
                                "createdAt", notification.getCreatedAt().toString()
                        )));
            } catch (Exception e) {
                emitters.remove(userId);
            }
        }
    }

    @Transactional
    public void registerDeviceToken(Long userId, String token, String platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        deviceTokenRepository.findAllByUser(user).stream()
                .filter(t -> t.getToken().equals(token))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {},
                        () -> deviceTokenRepository.save(DeviceToken.builder()
                                .user(user)
                                .token(token)
                                .platform(platform)
                                .createdAt(LocalDateTime.now())
                                .build())
                );
    }

    @Transactional
    public void unregisterDeviceToken(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    public boolean isAlreadyNotified(User user, NotificationType type, Long refId) {
        if (refId == null) return false;
        return notificationRepository
                .findFirstByUserAndTypeAndRefIdAndCreatedAtAfter(
                        user, type, refId, LocalDateTime.now().minusDays(1))
                .isPresent();
    }

    public Long getUserIdFromToken(String token) {
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        return jwtProvider.verifyAccessTokenAndGetUserId(cleanToken);
    }
}
