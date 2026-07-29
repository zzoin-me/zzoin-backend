package com.hicct3.projectfinder.entity;

import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="jobCategory")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private JobCategoryCode categoryCode;

    @Column(nullable = false)
    private String name;
}
