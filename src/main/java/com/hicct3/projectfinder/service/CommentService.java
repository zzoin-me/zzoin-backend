package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.CommentResponseDTO;
import com.hicct3.projectfinder.dto.community.CreateCommentRequestDTO;
import com.hicct3.projectfinder.entity.Comment;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.CommentRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponseDTO createComment(Long userId, Long postId, CreateCommentRequestDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted()) {
            throw new GeneralException(ErrorCode.POST_DELETED);
        }

        Comment parent = null;
        int depth = 0;
        if (req.getParentId() != null) {
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.COMMENT_NOT_FOUND));
            if (parent.isDeleted()) {
                throw new GeneralException(ErrorCode.COMMENT_NOT_FOUND);
            }
            if (parent.getDepth() >= 1) {
                throw new GeneralException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
            depth = parent.getDepth() + 1;
        }

        Comment comment = Comment.builder()
                .content(req.getContent())
                .author(user)
                .post(post)
                .parent(parent)
                .depth(depth)
                .createdAt(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);
        post.increaseCommentCount();
        return CommentResponseDTO.of(saved, userId);
    }

    @Transactional
    public void updateComment(Long userId, Long commentId, String content) {
        Comment comment = getOwnedComment(userId, commentId);
        comment.setContent(content);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getOwnedComment(userId, commentId);
        comment.setDeletedAt(LocalDateTime.now());
        comment.getPost().decreaseCommentCount();
    }

    @Transactional
    public List<CommentResponseDTO> getComments(Long postId, Long currentUserId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        List<CommentResponseDTO> dtos = comments.stream()
                .map(c -> CommentResponseDTO.of(c, currentUserId))
                .collect(Collectors.toList());

        Map<Long, CommentResponseDTO> byId = new LinkedHashMap<>();
        for (CommentResponseDTO dto : dtos) {
            byId.put(dto.getId(), dto);
        }

        List<CommentResponseDTO> roots = new ArrayList<>();
        for (CommentResponseDTO dto : dtos) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                CommentResponseDTO parentDto = byId.get(dto.getParentId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    private Comment getOwnedComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            throw new GeneralException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (comment.getAuthor() == null || !comment.getAuthor().getUserId().equals(userId)) {
            throw new GeneralException(ErrorCode.NOT_COMMENT_AUTHOR);
        }
        return comment;
    }
}
