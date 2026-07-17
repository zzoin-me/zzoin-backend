package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name="email_verifications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email", "type"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public EmailVerification(String email, VerificationType type, String code, User user, LocalDateTime expiredAt) {
        this.email = email;
        this.type = type;
        this.code = code;
        this.user = user;
        this.expiredAt = expiredAt;
    }

    public void update(String code, User user, LocalDateTime expiredAt) {
        this.code = code;
        this.user = user;
        this.expiredAt = expiredAt;
    }
}