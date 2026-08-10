package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectChatRead;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectChatReadRepository extends JpaRepository<ProjectChatRead, Long> {
    Optional<ProjectChatRead> findByProjectAndUser(Project project, User user);
}
