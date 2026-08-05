package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import com.hicct3.projectfinder.entity.enums.SortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface ProjectRepositoryCustom {
    Page<Project> searchProjects(
            SortType sortType,
            String keyword,
            JobCategoryCode category,
            String name,
            Integer maxDays,
            Integer minCount,
            Integer maxCount,
            GoalType goal,
            Boolean recruitingOnly,
            Pageable pageable
    );

    Map<JobCategoryCode, Long> countProjectsPerCategory();
}
