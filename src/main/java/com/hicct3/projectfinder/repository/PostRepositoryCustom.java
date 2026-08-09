package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PostRepositoryCustom {
    Page<Post> searchPosts(
            PostBoardType board,
            PostSortType sortType,
            String keyword,
            Long currentUserId,
            Pageable pageable
    );

    long countLikesByPostId(Long postId);
    long countCommentsByPostId(Long postId);

    Map<Long, Long> countLikesByPostIds(List<Long> postIds);
    Map<Long, Long> countCommentsByPostIds(List<Long> postIds);
    List<Long> findLikedPostIds(Long userId, List<Long> postIds);
    List<Long> findSavedPostIds(Long userId, List<Long> postIds);
}
