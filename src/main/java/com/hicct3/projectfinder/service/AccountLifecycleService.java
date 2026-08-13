package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.DeviceTokenRepository;
import com.hicct3.projectfinder.repository.EmailVerificationRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AccountLifecycleService {
    private static final int RECOVERY_DAYS = 30;

    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final NotificationService notificationService;
    private final R2ImageStorageService r2ImageStorageService;
    private final Clock clock;

    public boolean finalizeIfExpired(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getDeletedAt() != null
                && user.getDeletionFinalizedAt() == null
                && !now.isBefore(user.getDeletedAt().plusDays(RECOVERY_DAYS))) {
            finalizeWithdrawal(user, now);
            return true;
        }
        return false;
    }

    public boolean isRecoverable(User user) {
        return user.isRecoverable(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizeExpiredAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        return finalizeIfExpired(user);
    }

    @Transactional
    public User recover(User user) {
        if (!isRecoverable(user)) {
            throw new GeneralException(ErrorCode.ACCOUNT_RECOVERY_EXPIRED);
        }
        user.recover();
        return user;
    }

    @Transactional
    public void deactivateActiveSessions(Long userId) {
        deviceTokenRepository.deleteAllByUser_UserId(userId);
        notificationService.disconnectUser(userId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void finalizeExpiredAccounts() {
        LocalDateTime now = LocalDateTime.now(clock);
        userRepository
                .findAllByDeletedAtBeforeAndDeletionFinalizedAtIsNull(now.minusDays(RECOVERY_DAYS))
                .forEach(user -> finalizeWithdrawal(user, now));
    }

    private void finalizeWithdrawal(User user, LocalDateTime finalizedAt) {
        Long userId = user.getUserId();
        List<String> personalEmails = Stream.of(user.getEmail(), user.getVerifiedEmail())
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .distinct()
                .toList();

        deactivateActiveSessions(userId);
        emailVerificationRepository.deleteAllByUser_UserId(userId);
        if (!personalEmails.isEmpty()) {
            emailVerificationRepository.deleteAllByEmailIn(personalEmails);
        }
        if (user.getProfileUrl() != null) {
            r2ImageStorageService.deleteManagedImages(List.of(user.getProfileUrl()));
        }

        user.finalizeWithdrawal(finalizedAt);
    }
}
