package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findAllByUser(User user);
    List<ProjectMember> findAllByUserAndProject(User user, Project project);

    boolean existsByUserAndProject(User user, Project project);

    List<ProjectMember> findAllByProject(Project project);

    Optional<ProjectMember> findByUserAndProject(User user, Project project);

    long countByProject(Project project);

    long countByProjectAndUserNot(Project project, User user);

    Page<ProjectMember> findByUserAndProject_Status(
            User user,
            ProjectStatus status,
            Pageable pageable
    );
}