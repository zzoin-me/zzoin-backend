package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.PostDetailResponseDTO;
import com.hicct3.projectfinder.dto.community.PostPreviewResponseDTO;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.PostLikeRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.PostSaveRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;

    @Transactional(readOnly = true)
    public Page<PostPreviewResponseDTO> getPostList(
            PostBoardType board,
            PostSortType sortType,
            String keyword,
            Long currentUserId,
            Pageable pageable
    ) {
        Page<Post> posts = postRepository.searchPosts(board, sortType, keyword, currentUserId, pageable);

        List<Post> content = posts.getContent();
        List<Long> postIds = content.stream().map(Post::getId).collect(Collectors.toList());

        Map<Long, Long> likeCounts = postRepository.countLikesByPostIds(postIds);
        Map<Long, Long> commentCounts = postRepository.countCommentsByPostIds(postIds);
        Set<Long> likedIds = new HashSet<>(postRepository.findLikedPostIds(currentUserId, postIds));
        Set<Long> savedIds = new HashSet<>(postRepository.findSavedPostIds(currentUserId, postIds));

        List<PostPreviewResponseDTO> dtos = content.stream().map(post -> {
            int likeCount = likeCounts.getOrDefault(post.getId(), 0L).intValue();
            int commentCount = commentCounts.getOrDefault(post.getId(), 0L).intValue();
            Boolean likedByMe = currentUserId != null && likedIds.contains(post.getId());
            Boolean savedByMe = currentUserId != null && savedIds.contains(post.getId());
            return PostPreviewResponseDTO.of(post, likeCount, commentCount, likedByMe, savedByMe);
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    @Transactional
    public PostDetailResponseDTO getPostDetail(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorCode.POST_DELETED);
        }

        post.increaseViewCount();

        long likeCount = postRepository.countLikesByPostId(postId);
        long commentCount = postRepository.countCommentsByPostId(postId);
        Boolean likedByMe = currentUserId != null && postLikeRepository.existsByUser_UserIdAndPost_Id(currentUserId, postId);
        Boolean savedByMe = currentUserId != null && postSaveRepository.existsByUser_UserIdAndPost_Id(currentUserId, postId);

        return PostDetailResponseDTO.of(post, (int) likeCount, (int) commentCount, likedByMe, savedByMe, currentUserId);
    }
}
