package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.entity.Notification;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.global.JwtProvider;
import com.hicct3.projectfinder.repository.DeviceTokenRepository;
import com.hicct3.projectfinder.repository.NotificationRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FcmPushService fcmPushService;
    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void deliversNotificationOnlyAfterTransactionCommit() {
        User user = User.builder().userId(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(10L);
            return notification;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            notificationService.createNotification(
                    1L,
                    NotificationType.APPLICATION_RECEIVED,
                    "지원 알림",
                    "새 지원자가 있습니다.",
                    "/projects/1/manage#applicants",
                    20L
            );

            verify(fcmPushService, never()).sendToUser(any(), any(), any(), any());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(fcmPushService).sendToUser(
                    1L,
                    "지원 알림",
                    "새 지원자가 있습니다.",
                    "/projects/1/manage#applicants"
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsMultipleSseConnectionsForSameUser() throws Exception {
        notificationService.subscribe(1L);
        notificationService.subscribe(1L);

        Field emittersField = NotificationService.class.getDeclaredField("emitters");
        emittersField.setAccessible(true);
        Map<Long, Set<SseEmitter>> emitters =
                (Map<Long, Set<SseEmitter>>) emittersField.get(notificationService);

        assertEquals(2, emitters.get(1L).size());
    }

    @Test
    void marksOnlyOwnedNotificationAsRead() {
        Notification notification = Notification.builder().id(10L).isRead(false).build();
        when(notificationRepository.findByIdAndUser_UserId(10L, 1L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        assertEquals(true, notification.getIsRead());
        verify(notificationRepository).findByIdAndUser_UserId(10L, 1L);
    }

    @Test
    void marksEveryUnreadNotificationAsReadWithBulkUpdate() {
        when(userRepository.existsById(1L)).thenReturn(true);

        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    void reassignsExistingDeviceTokenToCurrentUser() {
        User previousUser = User.builder().userId(1L).build();
        User currentUser = User.builder().userId(2L).build();
        com.hicct3.projectfinder.entity.DeviceToken token =
                com.hicct3.projectfinder.entity.DeviceToken.builder()
                        .user(previousUser)
                        .token("device-token")
                        .platform("ANDROID")
                        .createdAt(java.time.LocalDateTime.now().minusDays(1))
                        .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(currentUser));
        when(deviceTokenRepository.findByToken("device-token")).thenReturn(Optional.of(token));

        notificationService.registerDeviceToken(2L, "device-token", "IOS");

        assertEquals(2L, token.getUser().getUserId());
        assertEquals("IOS", token.getPlatform());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void rejectsNotificationOwnedByAnotherUser() {
        when(notificationRepository.findByIdAndUser_UserId(10L, 1L))
                .thenReturn(Optional.empty());

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> notificationService.markAsRead(1L, 10L)
        );

        assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void unregistersOnlyOwnedDeviceToken() {
        when(deviceTokenRepository.deleteByTokenAndUser_UserId("device-token", 1L))
                .thenReturn(1L);

        notificationService.unregisterDeviceToken(1L, "device-token");

        verify(deviceTokenRepository).deleteByTokenAndUser_UserId("device-token", 1L);
    }

    @Test
    void rejectsDeviceTokenOwnedByAnotherUser() {
        when(deviceTokenRepository.deleteByTokenAndUser_UserId("device-token", 1L))
                .thenReturn(0L);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> notificationService.unregisterDeviceToken(1L, "device-token")
        );

        assertEquals(ErrorCode.DEVICE_TOKEN_NOT_FOUND, exception.getErrorCode());
    }
}
