package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Notification;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Notification> findByIdAndUser_UserId(Long id, Long userId);
    long countByUserAndIsReadFalse(User user);

    Optional<Notification> findFirstByUserAndTypeAndRefIdAndCreatedAtAfter(
            User user, com.hicct3.projectfinder.entity.enums.NotificationType type, Long refId, LocalDateTime createdAt);
}
