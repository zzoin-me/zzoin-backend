package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {
    Page<Post> searchPosts(
            PostBoardType board,
            PostSortType sortType,
            String keyword,
            Long currentUserId,
            Pageable pageable
    );

    List<Long> findLikedPostIds(Long userId, List<Long> postIds);
    List<Long> findSavedPostIds(Long userId, List<Long> postIds);
}
