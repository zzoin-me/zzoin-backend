package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import com.hicct3.projectfinder.entity.enums.SortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProjectRepositoryCustom {
    Page<Project> searchProjects(
            SortType sortType,
            String keyword,
            List<RecruitmentCategory> categories,
            List<String> names,
            Integer maxDays,
            Integer minCount,
            Integer maxCount,
            List<GoalType> goals,
            Boolean recruitingOnly,
            Pageable pageable
    );

    Map<RecruitmentCategory, Long> countProjectsPerCategory();

    Page<Project> findRecommendProjects(Long userId, List<String> userFields, Pageable pageable);

    Page<Project> findPopularProjects(Pageable pageable);
}
