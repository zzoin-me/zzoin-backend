package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findAllByUser(User user);
    List<ProjectMember> findAllByProject(Project project);
    List<ProjectMember> findAllByProjectAndStatus(Project project, MemberStatus status);
}