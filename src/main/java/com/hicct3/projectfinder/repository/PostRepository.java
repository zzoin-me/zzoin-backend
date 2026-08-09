package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
}
