package com.hicct3.projectfinder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="jobRole")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean isCustom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory jobCategory;

    public JobRole(String name, Boolean isCustom, JobCategory jobCategory) {
        this.name = name;
        this.isCustom = isCustom;
        this.jobCategory = jobCategory;
    }
}
