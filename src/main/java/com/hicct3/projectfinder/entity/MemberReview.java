package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "memberReviews",
        indexes = {
                @Index(name = "idx_member_review_target_created", columnList = "target_id, created_at"),
                @Index(name = "idx_member_review_author_created", columnList = "author_id, created_at")
        },
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"project_id", "author_id", "target_id"}
        )
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer contribution;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer participation;

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer responsibility;

    @Column
    @Size(max = 200, message = "코멘트는 200자 이하여야합니다.")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime hiddenAt;

    public Double getAverage() {
        return (contribution + participation + responsibility) / 3.0;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    public void setHidden(boolean hidden, LocalDateTime now) {
        hiddenAt = hidden ? now : null;
    }
}
