package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountLifecycleService {
    private static final int RECOVERY_DAYS = 30;

    private final UserRepository userRepository;
    private final Clock clock;

    public boolean finalizeIfExpired(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getDeletedAt() != null
                && user.getDeletionFinalizedAt() == null
                && !now.isBefore(user.getDeletedAt().plusDays(RECOVERY_DAYS))) {
            user.finalizeWithdrawal(now);
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

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void finalizeExpiredAccounts() {
        LocalDateTime now = LocalDateTime.now(clock);
        userRepository
                .findAllByDeletedAtBeforeAndDeletionFinalizedAtIsNull(now.minusDays(RECOVERY_DAYS))
                .forEach(user -> user.finalizeWithdrawal(now));
    }
}
