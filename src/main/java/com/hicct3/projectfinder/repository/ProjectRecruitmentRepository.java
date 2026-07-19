package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.SchoolDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRecruitmentRepository extends JpaRepository<ProjectRecruitment, Long> {
    void deleteAllByProject(Project project);
    Optional<ProjectRecruitment> findByProject(Project project);
}
