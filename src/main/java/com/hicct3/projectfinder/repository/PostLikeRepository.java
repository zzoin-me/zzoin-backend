package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUser_UserIdAndPost_Id(Long userId, Long postId);
    boolean existsByUser_UserIdAndPost_Id(Long userId, Long postId);
}
