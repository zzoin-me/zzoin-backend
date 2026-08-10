package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.CommentPageResponseDTO;
import com.hicct3.projectfinder.dto.community.CreateCommentRequestDTO;
import com.hicct3.projectfinder.entity.Comment;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.global.ErrorCode;
import com.hicct3.projectfinder.global.GeneralException;
import com.hicct3.projectfinder.repository.CommentRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private Clock clock;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).nickName("작성자").build();
        post = Post.builder().id(10L).author(user).build();
    }

    @Test
    void rejectsParentCommentFromAnotherPost() {
        Post anotherPost = Post.builder().id(20L).author(user).build();
        Comment parent = Comment.builder()
                .id(5L)
                .post(anotherPost)
                .author(user)
                .depth(0)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(commentRepository.findById(5L)).thenReturn(Optional.of(parent));

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> commentService.createComment(
                        1L,
                        10L,
                        CreateCommentRequestDTO.builder()
                                .content("잘못된 대댓글")
                                .parentId(5L)
                                .build()));

        assertEquals(ErrorCode.COMMENT_NOT_FOUND, exception.getErrorCode());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void returnsRootCommentCursorWithReplies() {
        Comment first = comment(1L, null, "첫 댓글");
        Comment reply = comment(11L, first, "답글");
        Comment second = comment(2L, null, "두 번째 댓글");
        when(commentRepository.findRootIdsAfter(eq(10L), eq(null), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L, 3L));
        when(commentRepository.findTreesByRootIds(10L, List.of(1L, 2L)))
                .thenReturn(List.of(first, reply, second));

        CommentPageResponseDTO result = commentService.getComments(10L, 1L, null, 2);

        assertTrue(result.getHasNext());
        assertEquals(2L, result.getNextCursor());
        assertEquals(2, result.getComments().size());
        assertEquals(1, result.getComments().getFirst().getChildren().size());
    }

    private Comment comment(Long id, Comment parent, String content) {
        return Comment.builder()
                .id(id)
                .post(post)
                .parent(parent)
                .author(user)
                .content(content)
                .depth(parent == null ? 0 : 1)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0).plusMinutes(id))
                .build();
    }
}
