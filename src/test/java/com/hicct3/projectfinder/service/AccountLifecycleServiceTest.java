package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.User;
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
import static org.mockito.Mockito.when;

class AccountLifecycleServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-13T09:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private final AccountLifecycleService service = new AccountLifecycleService(
            userRepository,
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
        assertTrue(user.getNickName().startsWith("DELETED_USER_"));
        assertNotNull(user.getDeletionFinalizedAt());
    }

    private User userWithWithdrawalAt(LocalDateTime deletedAt) {
        return User.builder()
                .userId(1L)
                .nickName("회원")
                .email("member@example.com")
                .password("encoded")
                .provider("kakao")
                .providerId("kakao-id")
                .localLoginEnabled(true)
                .verified(true)
                .admin(false)
                .deletedAt(deletedAt)
                .build();
    }
}
