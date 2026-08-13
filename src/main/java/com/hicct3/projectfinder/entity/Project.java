package com.hicct3.projectfinder.entity;

import com.hicct3.projectfinder.dto.project.CreateProjectRequestDTO;
import com.hicct3.projectfinder.entity.enums.CollaborationType;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(
    name="projects",
    indexes = {
        @Index(name = "idx_recruitment_deadline", columnList = "recruitment_deadline"),
        @Index(
                name = "idx_project_status_deadline",
                columnList = "status, deleted_at, recruitment_deadline")
    }
)
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

    @Column(nullable = false, length = 500)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    public void increaseViewCount() { this.viewCount++; }

    public Boolean isRecruitmentClosed()
    {
        return isRecruitmentClosed(Clock.systemDefaultZone());
    }

    public Boolean isRecruitmentClosed(Clock clock)
    {
        return deletedAt != null
                || status != ProjectStatus.RECRUITING
                || !recruitmentDeadline.isAfter(LocalDateTime.now(clock));
    }

    public static Project create(CreateProjectRequestDTO req, User user, Clock clock) {
        return Project.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .collaborationType(req.getCollaborationType())
                .communicationTool(req.getCommunicationTool())
                .meetingSchedule(req.getMeetingSchedule())
                .period(req.getPeriod())
                .recruitmentDeadline(req.getRecruitmentDeadline())
                .goal(req.getGoalType())
                .imageUrl(req.getImageUrl())
                .createdAt(LocalDateTime.now(clock))
                .updatedAt(LocalDateTime.now(clock))
                .deletedAt(null)
                .author(user)
                .status(ProjectStatus.RECRUITING)
                .build();
    }
}
