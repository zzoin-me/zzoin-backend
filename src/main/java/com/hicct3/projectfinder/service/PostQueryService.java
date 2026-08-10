package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.PostDetailResponseDTO;
import com.hicct3.projectfinder.dto.community.PostPreviewResponseDTO;
import com.hicct3.projectfinder.dto.community.ViewCountResultDTO;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.PostLikeRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.PostSaveRepository;
import com.hicct3.projectfinder.repository.PostViewRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final PostViewRepository postViewRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<PostPreviewResponseDTO> getPostList(
            PostBoardType board,
            PostSortType sortType,
            String keyword,
            Long currentUserId,
            Pageable pageable
    ) {
        if (board != PostBoardType.ALL && currentUserId == null) {
            return Page.empty(pageable);
        }
        Page<Post> posts = postRepository.searchPosts(board, sortType, keyword, currentUserId, pageable);

        List<Post> content = posts.getContent();
        List<Long> postIds = content.stream().map(Post::getId).toList();
        Set<Long> likedIds = new HashSet<>(postRepository.findLikedPostIds(currentUserId, postIds));
        Set<Long> savedIds = new HashSet<>(postRepository.findSavedPostIds(currentUserId, postIds));

        List<PostPreviewResponseDTO> dtos = content.stream().map(post -> {
            Boolean likedByMe = currentUserId != null && likedIds.contains(post.getId());
            Boolean savedByMe = currentUserId != null && savedIds.contains(post.getId());
            return PostPreviewResponseDTO.of(
                    post, post.getLikeCount(), post.getCommentCount(), likedByMe, savedByMe);
        }).toList();

        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PostDetailResponseDTO getPostDetail(Long postId, Long currentUserId) {
        Post post = postRepository.findByIdWithAuthor(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorCode.POST_DELETED);
        }

        Boolean likedByMe = currentUserId != null && postLikeRepository.existsByUser_UserIdAndPost_Id(currentUserId, postId);
        Boolean savedByMe = currentUserId != null && postSaveRepository.existsByUser_UserIdAndPost_Id(currentUserId, postId);

        return PostDetailResponseDTO.of(
                post, post.getLikeCount(), post.getCommentCount(), likedByMe, savedByMe, currentUserId);
    }

    @Transactional
    public ViewCountResultDTO recordView(Long postId, Long currentUserId, String viewerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorCode.POST_DELETED);
        }

        String viewerKey = currentUserId != null
                ? "user:" + currentUserId
                : "guest:" + normalizeViewerId(viewerId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime viewedHour = now.truncatedTo(ChronoUnit.HOURS);
        int inserted = postViewRepository.insertIfAbsent(
                postId, viewerKey, viewedHour, now);
        if (inserted > 0) {
            postRepository.increaseViewCount(postId);
        }
        return ViewCountResultDTO.of(inserted > 0);
    }

    private String normalizeViewerId(String viewerId) {
        if (viewerId == null || viewerId.isBlank()) {
            return "anonymous";
        }
        String normalized = viewerId.replaceAll("[^A-Za-z0-9_-]", "");
        if (normalized.isBlank()) {
            return "anonymous";
        }
        return normalized.substring(0, Math.min(normalized.length(), 64));
    }
}
