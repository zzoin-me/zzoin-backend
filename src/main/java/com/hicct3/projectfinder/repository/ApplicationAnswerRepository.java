package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.ApplicationAnswer;
import com.hicct3.projectfinder.entity.ProjectApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {
    List<ApplicationAnswer> findAllByApplicationIn(Collection<ProjectApplication> applications);
    List<ApplicationAnswer> findAllByApplication(ProjectApplication application);
    void deleteAllByApplication(ProjectApplication application);
}
