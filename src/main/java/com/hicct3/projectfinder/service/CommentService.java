package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.CommentResponseDTO;
import com.hicct3.projectfinder.dto.community.CommentPageResponseDTO;
import com.hicct3.projectfinder.dto.community.CreateCommentRequestDTO;
import com.hicct3.projectfinder.entity.Comment;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.NotificationType;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.CommentRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

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
            if (parent.getPost() == null || !parent.getPost().getId().equals(postId)) {
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
                .createdAt(LocalDateTime.now(clock))
                .build();

        Comment saved = commentRepository.save(comment);
        postRepository.increaseCommentCount(postId);

        if (parent != null) {
            if (!parent.getAuthor().getUserId().equals(userId) && !parent.getAuthor().getUserId().equals(post.getAuthor().getUserId())) {
                notificationService.createNotification(
                        parent.getAuthor().getUserId(),
                        NotificationType.COMMENT_REPLY,
                        "회원님의 댓글에 대댓글이 달렸어요",
                        user.getNickName() + "님이 회원님의 댓글에 답글을 남겼어요.",
                        "/community/" + post.getId(),
                        post.getId());
            }
        } else {
            if (!post.getAuthor().getUserId().equals(userId)) {
                notificationService.createNotification(
                        post.getAuthor().getUserId(),
                        NotificationType.POST_COMMENT,
                        "회원님의 게시글에 댓글이 달렸어요",
                        user.getNickName() + "님이 '" + post.getTitle() + "'에 댓글을 남겼어요.",
                        "/community/" + post.getId(),
                        post.getId());
            }
        }

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
        comment.setDeletedAt(LocalDateTime.now(clock));
        postRepository.decreaseCommentCount(comment.getPost().getId());
    }

    @Transactional
    public CommentPageResponseDTO getComments(
            Long postId,
            Long currentUserId,
            Long afterId,
            int size
    ) {
        int pageSize = Math.max(1, Math.min(size, 50));
        List<Long> fetchedRootIds = commentRepository.findRootIdsAfter(
                postId, afterId, PageRequest.of(0, pageSize + 1));
        boolean hasNext = fetchedRootIds.size() > pageSize;
        List<Long> rootIds = hasNext
                ? fetchedRootIds.subList(0, pageSize)
                : fetchedRootIds;
        if (rootIds.isEmpty()) {
            return CommentPageResponseDTO.builder()
                    .comments(List.of())
                    .nextCursor(null)
                    .hasNext(false)
                    .build();
        }

        List<Comment> comments = commentRepository.findTreesByRootIds(postId, rootIds);
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
        return CommentPageResponseDTO.builder()
                .comments(roots)
                .nextCursor(hasNext ? rootIds.get(rootIds.size() - 1) : null)
                .hasNext(hasNext)
                .build();
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
