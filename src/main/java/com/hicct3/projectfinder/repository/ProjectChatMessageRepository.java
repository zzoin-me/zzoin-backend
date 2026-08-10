package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectChatMessageRepository extends JpaRepository<ProjectChatMessage, Long> {
    Page<ProjectChatMessage> findAllByProjectOrderByIdDesc(Project project, Pageable pageable);
    Page<ProjectChatMessage> findAllByProjectAndIdLessThanOrderByIdDesc(
            Project project,
            Long beforeId,
            Pageable pageable);
    Optional<ProjectChatMessage> findTopByProjectOrderByIdDesc(Project project);
    Optional<ProjectChatMessage> findByIdAndProject(Long id, Project project);
    long countByProjectAndIdGreaterThan(Project project, Long messageId);
}
