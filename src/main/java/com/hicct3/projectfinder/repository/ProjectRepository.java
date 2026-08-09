package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.Stack;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom  {
    Page<Project> findAllByAuthorAndDeletedAtIsNullOrderByIdDesc(User author, Pageable pageable);
    Page<Project> findAllByAuthorAndDeletedAtIsNullAndStatusOrderByIdDesc(User author, ProjectStatus status, Pageable pageable);
    Page<Project> findAllByAuthorAndDeletedAtIsNullAndStatusNotOrderByIdDesc(User author, ProjectStatus status, Pageable pageable);
}
