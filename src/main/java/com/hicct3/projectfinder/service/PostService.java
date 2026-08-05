package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.CreatePostRequestDTO;
import com.hicct3.projectfinder.dto.community.ToggleResultDTO;
import com.hicct3.projectfinder.dto.community.UpdatePostRequestDTO;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.PostLike;
import com.hicct3.projectfinder.entity.PostSave;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.PostLikeRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.PostSaveRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createPost(Long userId, CreatePostRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        Post post = Post.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .author(user)
                .viewCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return postRepository.save(post).getId();
    }

    @Transactional
    public void updatePost(Long userId, Long postId, UpdatePostRequestDTO req) {
        Post post = getOwnedPost(userId, postId);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            post.setTitle(req.getTitle());
        }
        if (req.getContent() != null && !req.getContent().isBlank()) {
            post.setContent(req.getContent());
        }
        post.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getOwnedPost(userId, postId);
        post.setDeletedAt(LocalDateTime.now());
    }

    @Transactional
    public ToggleResultDTO toggleLike(Long userId, Long postId) {
        Post post = getActivePost(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var existing = postLikeRepository.findByUser_UserIdAndPost_Id(userId, postId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            return ToggleResultDTO.of(false);
        }

        PostLike like = PostLike.builder()
                .user(user)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();
        postLikeRepository.save(like);
        return ToggleResultDTO.of(true);
    }

    @Transactional
    public ToggleResultDTO toggleSave(Long userId, Long postId) {
        Post post = getActivePost(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        var existing = postSaveRepository.findByUser_UserIdAndPost_Id(userId, postId);
        if (existing.isPresent()) {
            postSaveRepository.delete(existing.get());
            return ToggleResultDTO.of(false);
        }

        PostSave save = PostSave.builder()
                .user(user)
                .post(post)
                .createdAt(LocalDateTime.now())
                .build();
        postSaveRepository.save(save);
        return ToggleResultDTO.of(true);
    }

    private Post getActivePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorCode.POST_DELETED);
        }
        return post;
    }

    private Post getOwnedPost(Long userId, Long postId) {
        Post post = getActivePost(postId);
        if (post.getAuthor() == null || !post.getAuthor().getUserId().equals(userId)) {
            throw new GeneralException(ErrorCode.NOT_POST_AUTHOR);
        }
        return post;
    }
}
