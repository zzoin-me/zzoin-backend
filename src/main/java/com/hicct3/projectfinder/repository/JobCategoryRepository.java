package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.JobCategory;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
    Optional<JobCategory> findByCategoryCode(JobCategoryCode categoryCode);
}