package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.MemberReview;
import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberReviewRepository extends JpaRepository<MemberReview, Long> {
    List<MemberReview> findAllByAuthorAndProject(User author, Project project);
    Page<MemberReview> findAllByAuthor(User author, Pageable pageable);
    Page<MemberReview> findAllByTargetAndHiddenAtIsNull(User target, Pageable pageable);
    List<MemberReview> findAllByTargetAndHiddenAtIsNull(User target);
    List<MemberReview> findAllByProject(Project project);
    boolean existsByAuthorAndProjectAndTarget(User author, Project project, User target);
    long countByAuthorAndProject(User author, Project project);
    long countByProject(Project project);

}
