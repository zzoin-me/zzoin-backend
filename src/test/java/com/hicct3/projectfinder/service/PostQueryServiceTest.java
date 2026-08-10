package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.dto.community.PostDetailResponseDTO;
import com.hicct3.projectfinder.dto.community.PostPreviewResponseDTO;
import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import com.hicct3.projectfinder.repository.PostLikeRepository;
import com.hicct3.projectfinder.repository.PostRepository;
import com.hicct3.projectfinder.repository.PostSaveRepository;
import com.hicct3.projectfinder.repository.PostViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {
    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostSaveRepository postSaveRepository;
    @Mock private PostViewRepository postViewRepository;
    @Mock private Clock clock;

    @InjectMocks
    private PostQueryService postQueryService;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder().userId(1L).nickName("작성자").build();
        post = Post.builder()
                .id(10L)
                .title("제목")
                .content("내용")
                .author(author)
                .viewCount(7)
                .likeCount(4)
                .commentCount(3)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .build();
    }

    @Test
    void listUsesStoredCountersAndPersonalFlags() {
        PageRequest pageable = PageRequest.of(0, 9);
        when(postRepository.searchPosts(
                PostBoardType.ALL, PostSortType.LATEST, null, 2L, pageable))
                .thenReturn(new PageImpl<>(List.of(post), pageable, 1));
        when(postRepository.findLikedPostIds(2L, List.of(10L))).thenReturn(List.of(10L));
        when(postRepository.findSavedPostIds(2L, List.of(10L))).thenReturn(List.of());

        PostPreviewResponseDTO result = postQueryService.getPostList(
                PostBoardType.ALL, PostSortType.LATEST, null, 2L, pageable)
                .getContent().getFirst();

        assertEquals(4, result.getLikeCount());
        assertEquals(3, result.getCommentCount());
        assertTrue(result.getLikedByMe());
        assertFalse(result.getSavedByMe());
    }

    @Test
    void detailReadDoesNotIncreaseViewCount() {
        when(postRepository.findByIdWithAuthor(10L)).thenReturn(Optional.of(post));

        PostDetailResponseDTO result = postQueryService.getPostDetail(10L, 2L);

        assertEquals(7, result.getViewCount());
        verify(postRepository, never()).increaseViewCount(10L);
    }

    @Test
    void recordsOnlyFirstHourlyView() {
        stubClock();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postViewRepository.insertIfAbsent(eq(10L), eq("user:2"), any(), any()))
                .thenReturn(1, 0);

        assertTrue(postQueryService.recordView(10L, 2L, null).isCounted());
        assertFalse(postQueryService.recordView(10L, 2L, null).isCounted());

        verify(postRepository).increaseViewCount(10L);
    }

    private void stubClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-08-10T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }
}
