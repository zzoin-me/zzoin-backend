package com.hicct3.projectfinder.repository;

import com.hicct3.projectfinder.entity.Post;
import com.hicct3.projectfinder.entity.enums.PostBoardType;
import com.hicct3.projectfinder.entity.enums.PostSortType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:postrepository;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.quartz.job-store-type=jdbc",
        "spring.quartz.jdbc.initialize-schema=always",
        "spring.quartz.auto-startup=false",
        "app.deadline.recovery.enabled=false"
})
@Transactional
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void popularSortUsesNumericScoreWithoutQueryTypeError() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 16, 0);
        postRepository.saveAll(List.of(
                post("조회수 게시글", 20, 0, 0, now.minusMinutes(2)),
                post("댓글 게시글", 0, 0, 8, now.minusMinutes(1)),
                post("좋아요 게시글", 0, 5, 0, now)
        ));

        Page<Post> result = postRepository.searchPosts(
                PostBoardType.ALL,
                PostSortType.POPULAR,
                null,
                null,
                PageRequest.of(0, 10));

        assertEquals(
                List.of("좋아요 게시글", "댓글 게시글", "조회수 게시글"),
                result.getContent().stream().map(Post::getTitle).toList());
    }

    private Post post(
            String title,
            int viewCount,
            int likeCount,
            int commentCount,
            LocalDateTime createdAt
    ) {
        return Post.builder()
                .title(title)
                .content("내용")
                .viewCount(viewCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
