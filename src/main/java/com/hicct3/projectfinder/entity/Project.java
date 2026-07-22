package com.hicct3.projectfinder.entity;

import com.hicct3.projectfinder.entity.enums.CollaborationType;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="projects")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationType collaborationType;

    @Column(nullable = false)
    private String communicationTool;

    @Column
    private String meetingSchedule;

    @Column
    private String period;

    @Column(nullable = false)
    private LocalDateTime recruitmentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalType goal;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User author;

    @Column(nullable = false)
    private ProjectStatus status;

    public Boolean isRecruitmentClosed()
    {
        return status != ProjectStatus.RECRUITING || recruitmentDeadline.isBefore(LocalDateTime.now());
    }
}
