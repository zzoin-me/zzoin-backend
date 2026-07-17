package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.EmailVerification;
import com.hicct3.projectfinder.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmail(String email);
    Optional<EmailVerification> findByEmailAndType(String email, VerificationType type);
}