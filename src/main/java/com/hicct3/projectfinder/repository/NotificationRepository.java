package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Notification;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Notification> findByIdAndUser_UserId(Long id, Long userId);
    long countByUserAndIsReadFalse(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    Optional<Notification> findFirstByUserAndTypeAndRefIdAndCreatedAtAfter(
            User user, com.hicct3.projectfinder.entity.enums.NotificationType type, Long refId, LocalDateTime createdAt);
}
