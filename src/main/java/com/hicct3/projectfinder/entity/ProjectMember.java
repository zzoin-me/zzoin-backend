package com.hicct3.projectfinder.entity;

import com.hicct3.projectfinder.entity.enums.MemberStatus;
import com.hicct3.projectfinder.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(
        name="projectMembers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_member_project_user",
                columnNames = {"project_id", "user_id"}
        )
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = true)
    private ProjectRecruitment recruitment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public String getJobName() {
        return recruitment != null ? recruitment.getJobRole().getName() : role.getName();
    }
}
