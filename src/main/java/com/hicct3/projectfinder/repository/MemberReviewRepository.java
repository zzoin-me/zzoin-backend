package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.MemberReview;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberReviewRepository extends JpaRepository<MemberReview, Long> {
    List<MemberReview> findAllByAuthorAndProject(User author, Project project);
    List<MemberReview> findAllByTarget(User target);
    List<MemberReview> findAllByProject(Project project);
    Boolean existsByAuthorAndProject(User author, Project project);
    long countByAuthorAndProject(User author, Project project);
    long countByProject(Project project);

}