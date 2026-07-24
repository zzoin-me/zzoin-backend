package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectApplication;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {
    boolean existsByUserAndRecruitment(User user, ProjectRecruitment recruitment);

    @Query("""
            select pa
            from ProjectApplication pa
            join fetch pa.user
            join fetch pa.recruitment r
            where r.project = :project
            """)
    List<ProjectApplication> findAllByProject(Project project);

    Page<ProjectApplication> findAllByUser(User user, Pageable pageable);

    Page<ProjectApplication> findAllByUserAndStatus(User user, ApplicationStatus status, Pageable pageable);
}