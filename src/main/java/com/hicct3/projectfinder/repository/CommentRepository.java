package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    long countByPostIdAndDeletedAtIsNull(Long postId);

    @Query("""
            select c.id from Comment c
            where c.post.id = :postId
              and c.parent is null
              and (:afterId is null or c.id > :afterId)
            order by c.id asc
            """)
    List<Long> findRootIdsAfter(
            @Param("postId") Long postId,
            @Param("afterId") Long afterId,
            Pageable pageable
    );

    @Query("""
            select distinct c from Comment c
            left join fetch c.author
            left join fetch c.parent
            where c.post.id = :postId
              and (c.id in :rootIds or c.parent.id in :rootIds)
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findTreesByRootIds(
            @Param("postId") Long postId,
            @Param("rootIds") List<Long> rootIds
    );
}
