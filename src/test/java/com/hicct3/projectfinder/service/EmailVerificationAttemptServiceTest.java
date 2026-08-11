package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.entity.EmailVerification;
import com.hicct3.projectfinder.entity.enums.VerificationType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.EmailVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationAttemptServiceTest {

    @Mock private EmailVerificationRepository repository;

    private BCryptPasswordEncoder passwordEncoder;
    private EmailVerificationAttemptService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-11T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new EmailVerificationAttemptService(repository, passwordEncoder, clock);
    }

    @Test
    void consumesHashedCodeOnSuccessfulVerification() {
        String rawCode = "123456";
        String encodedCode = passwordEncoder.encode(rawCode);
        EmailVerification verification = verification(encodedCode);
        when(repository.findByEmailAndType("user@example.com", VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));

        service.verifyAndConsume(
                "user@example.com", VerificationType.SIGNUP, rawCode, null);

        assertNotEquals(rawCode, verification.getCode());
        verify(repository).delete(verification);
    }

    @Test
    void deletesCodeAfterFiveFailures() {
        EmailVerification verification = verification(passwordEncoder.encode("123456"));
        when(repository.findByEmailAndType("user@example.com", VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));

        for (int i = 0; i < 4; i++) {
            GeneralException exception = assertThrows(
                    GeneralException.class,
                    () -> service.verifyAndConsume(
                            "user@example.com", VerificationType.SIGNUP, "000000", null));
            assertEquals(ErrorCode.EMAIL_CODE_MISMATCH, exception.getErrorCode());
        }
        verify(repository, never()).delete(verification);

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> service.verifyAndConsume(
                        "user@example.com", VerificationType.SIGNUP, "000000", null));

        assertEquals(ErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED, exception.getErrorCode());
        verify(repository).delete(verification);
    }

    private EmailVerification verification(String encodedCode) {
        return new EmailVerification(
                "user@example.com",
                VerificationType.SIGNUP,
                encodedCode,
                null,
                LocalDateTime.of(2026, 8, 11, 9, 5));
    }
}
