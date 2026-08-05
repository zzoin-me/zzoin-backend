package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    long countByPostIdAndDeletedAtIsNull(Long postId);
}
