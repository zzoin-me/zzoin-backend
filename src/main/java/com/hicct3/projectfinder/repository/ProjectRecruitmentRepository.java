package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.SchoolDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRecruitmentRepository extends JpaRepository<ProjectRecruitment, Long> {
    List<ProjectRecruitment> findByProject(Project project);

    List<ProjectRecruitment> findAllByProject(Project project);
    List<ProjectRecruitment> findAllByProjectAndDeletedAtIsNull(Project project);
    List<ProjectRecruitment> findAllByProjectInAndDeletedAtIsNull(Collection<Project> projects);
    Optional<ProjectRecruitment> findByIdAndProjectAndDeletedAtIsNull(Long id, Project project);
}
