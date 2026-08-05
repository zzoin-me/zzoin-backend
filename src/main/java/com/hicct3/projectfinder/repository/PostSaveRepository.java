package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.PostSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostSaveRepository extends JpaRepository<PostSave, Long> {
    Optional<PostSave> findByUser_UserIdAndPost_Id(Long userId, Long postId);
    boolean existsByUser_UserIdAndPost_Id(Long userId, Long postId);
}
