package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PostViewRepository extends JpaRepository<PostView, Long> {
    long deleteByViewedHourBefore(LocalDateTime cutoff);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO post_views (post_id, viewer_key, viewed_hour, created_at)
            VALUES (:postId, :viewerKey, :viewedHour, :createdAt)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("postId") Long postId,
            @Param("viewerKey") String viewerKey,
            @Param("viewedHour") LocalDateTime viewedHour,
            @Param("createdAt") LocalDateTime createdAt
    );
}
