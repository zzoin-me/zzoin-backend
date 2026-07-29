package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="projectRecruitments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectRecruitment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    private Integer applicantCount;

    @Column(nullable = false)
    private Integer recruitmentCount;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String preferred;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_role_id")
    private JobRole jobRole;

    public void update(JobRole jobRole, Integer recruitmentCount, String qualification, String preferred)
    {
        this.jobRole = jobRole;
        this.recruitmentCount = recruitmentCount;
        this.qualification = qualification;
        this.preferred = preferred;
    }

    public void delete()
    {
        this.deletedAt = LocalDateTime.now();
    }
}
