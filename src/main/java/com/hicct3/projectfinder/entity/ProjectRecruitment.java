package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.Interval;

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

    @Column(nullable = false)
    private Integer currentCount;

    @Column(nullable = false)
    private Integer recruitmentCount;

    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String preferred;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
}
