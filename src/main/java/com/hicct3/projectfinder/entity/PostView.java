package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "viewer_key", "viewed_hour"})
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "viewer_key", nullable = false, length = 80)
    private String viewerKey;

    @Column(name = "viewed_hour", nullable = false)
    private LocalDateTime viewedHour;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
