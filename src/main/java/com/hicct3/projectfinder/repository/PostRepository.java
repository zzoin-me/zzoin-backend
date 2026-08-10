package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    @Query("select p from Post p left join fetch p.author where p.id = :postId")
    Optional<Post> findByIdWithAuthor(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :postId and p.deletedAt is null")
    int increaseViewCount(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :postId and p.deletedAt is null")
    int increaseLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query(value = """
            UPDATE posts SET like_count = GREATEST(like_count - 1, 0)
            WHERE id = :postId AND deleted_at IS NULL
            """, nativeQuery = true)
    int decreaseLikeCount(@Param("postId") Long postId);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + 1 where p.id = :postId and p.deletedAt is null")
    int increaseCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query(value = """
            UPDATE posts SET comment_count = GREATEST(comment_count - 1, 0)
            WHERE id = :postId AND deleted_at IS NULL
            """, nativeQuery = true)
    int decreaseCommentCount(@Param("postId") Long postId);
}
