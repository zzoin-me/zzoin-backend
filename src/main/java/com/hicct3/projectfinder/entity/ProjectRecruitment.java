package com.hicct3.projectfinder.entity;

import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
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
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentCategory category;

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
}
