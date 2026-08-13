package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens",
    uniqueConstraints = @UniqueConstraint(columnNames = "token"),
    indexes = @Index(name = "idx_device_token_user", columnList = "user_id")
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public void reassign(User nextUser, String nextPlatform, LocalDateTime reassignedAt) {
        this.user = nextUser;
        this.platform = nextPlatform;
        this.createdAt = reassignedAt;
    }
}
