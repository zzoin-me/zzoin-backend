package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.JobCategory;
import com.hicct3.projectfinder.entity.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRoleRepository extends JpaRepository<JobRole, Long> {

    List<JobRole> findAllByJobCategory(JobCategory jobCategory);
}