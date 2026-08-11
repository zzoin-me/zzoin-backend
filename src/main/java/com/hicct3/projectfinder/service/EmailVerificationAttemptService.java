package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.EmailVerification;
import com.hicct3.projectfinder.entity.enums.VerificationType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {

    private static final int MAX_FAILURES = 5;

    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = GeneralException.class)
    public void verifyAndConsume(
            String email,
            VerificationType type,
            String rawCode,
            Long expectedUserId) {
        EmailVerification verification = emailVerificationRepository.findByEmailAndType(email, type)
                .orElseThrow(() -> new GeneralException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.getExpiredAt().isBefore(LocalDateTime.now(clock))) {
            emailVerificationRepository.delete(verification);
            throw new GeneralException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        if (expectedUserId != null
                && (verification.getUser() == null
                || !Objects.equals(verification.getUser().getUserId(), expectedUserId))) {
            throw new GeneralException(ErrorCode.INVALID_USER);
        }

        if (!matches(rawCode, verification.getCode())) {
            int failures = verification.registerFailure();
            if (failures >= MAX_FAILURES) {
                emailVerificationRepository.delete(verification);
                throw new GeneralException(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
            }
            throw new GeneralException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        emailVerificationRepository.delete(verification);
    }

    private boolean matches(String rawCode, String encodedCode) {
        if (rawCode == null || encodedCode == null) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawCode, encodedCode);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
