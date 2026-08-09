package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.Stack;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectRepositoryCustom  {
    Page<Project> findAllByAuthorAndDeletedAtIsNull(User author, Pageable pageable);
    List<Project> findAllByAuthor(User author);

    @Query("""
            select p
            from Project p
            where p.author = :author
              and p.deletedAt is null
              and (
                    :status is null
                    or (:status = 'RECRUITING' and p.status = com.hicct3.projectfinder.entity.enums.ProjectStatus.RECRUITING)
                    or (:status = 'CLOSED' and p.status <> com.hicct3.projectfinder.entity.enums.ProjectStatus.RECRUITING)
              )
              and (
                    :hasApplicants = false
                    or exists (
                        select pa.id
                        from ProjectApplication pa
                        where pa.recruitment.project = p
                    )
              )
            """)
    Page<Project> findMyProjects(
            @Param("author") User author,
            @Param("status") String status,
            @Param("hasApplicants") boolean hasApplicants,
            Pageable pageable);
}
