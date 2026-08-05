package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectQuestionRepository extends JpaRepository<ProjectQuestion, Long> {
    List<ProjectQuestion> findAllByProjectAndDeletedAtIsNullOrderByIdAsc(Project project);
    List<ProjectQuestion> findAllByProjectInAndDeletedAtIsNull(Collection<Project> projects);
}
