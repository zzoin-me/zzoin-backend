package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.repository.DeviceTokenRepository;
import com.hicct3.projectfinder.repository.EmailVerificationRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountLifecycleServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    private final EmailVerificationRepository emailVerificationRepository = mock(EmailVerificationRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final R2ImageStorageService r2ImageStorageService = mock(R2ImageStorageService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-13T09:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final AccountLifecycleService service = new AccountLifecycleService(
            userRepository,
            deviceTokenRepository,
            emailVerificationRepository,
            notificationService,
            r2ImageStorageService,
            clock
    );

    @Test
    void accountCanRecoverWithinThirtyDays() {
        User user = userWithWithdrawalAt(LocalDateTime.of(2026, 7, 15, 18, 0, 1));

        assertTrue(service.isRecoverable(user));
        assertFalse(service.finalizeIfExpired(user));

        service.recover(user);

        assertFalse(user.isDeleted());
        assertNull(user.getDeletionFinalizedAt());
    }

    @Test
    void accountIsFinalizedAtThirtyDayBoundary() {
        User user = userWithWithdrawalAt(LocalDateTime.of(2026, 7, 14, 18, 0));

        assertFalse(service.isRecoverable(user));
        assertTrue(service.finalizeIfExpired(user));
        assertTrue(user.isDeleted());
        assertTrue(user.getEmail().startsWith("DELETED_EMAIL_"));
        assertNull(user.getProviderId());
        assertFalse(user.getLocalLoginEnabled());
    }

    @Test
    void expiredAccountCanBeFinalizedInIndependentTransactionEntryPoint() {
        User user = userWithWithdrawalAt(LocalDateTime.of(2026, 7, 14, 18, 0));
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        assertTrue(service.finalizeExpiredAccount(1L));
        assertTrue(user.getEmail().startsWith("DELETED_EMAIL_"));
        assertTrue(user.getNickName().startsWith("DELETED"));
        assertNotNull(user.getDeletionFinalizedAt());
        verify(deviceTokenRepository).deleteAllByUser_UserId(1L);
        verify(emailVerificationRepository).deleteAllByUser_UserId(1L);
        verify(emailVerificationRepository).deleteAllByEmailIn(java.util.List.of("member@example.com"));
        verify(notificationService).disconnectUser(1L);
        verify(r2ImageStorageService).deleteManagedImages(java.util.List.of("https://cdn.example/profile.png"));
    }

    @Test
    void anonymizedNicknameFitsTwentyCharacterDatabaseLimit() {
        User user = userWithWithdrawalAt(LocalDateTime.of(2026, 7, 14, 18, 0));
        user.setUserId(Long.MAX_VALUE);

        user.finalizeWithdrawal(LocalDateTime.of(2026, 8, 13, 18, 0));

        assertTrue(user.getNickName().length() <= 20);
        assertTrue(user.getNickName().startsWith("DELETED"));
    }

    private User userWithWithdrawalAt(LocalDateTime deletedAt) {
        return User.builder()
                .userId(1L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("kakao")
                .providerId("kakao-id")
                .profileUrl("https://cdn.example/profile.png")
                .localLoginEnabled(true)
                .verified(true)
                .admin(false)
                .deletedAt(deletedAt)
                .build();
    }
}
