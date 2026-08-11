package com.hicct3.projectfinder.dto.community;

import com.hicct3.projectfinder.entity.Post;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailResponseDTO {
    private Long id;
    private String title;
    private String content;
    private List<String> imageUrls;
    private AuthorDTO author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Boolean likedByMe;
    private Boolean savedByMe;
    private Boolean isMine;

    public static PostDetailResponseDTO of(Post post, Integer likeCount, Integer commentCount, Boolean likedByMe, Boolean savedByMe, Long currentUserId) {
        return PostDetailResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(post.getImageUrls())
                .author(AuthorDTO.from(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .viewCount(post.getViewCount())
                .likedByMe(likedByMe)
                .savedByMe(savedByMe)
                .isMine(post.getAuthor() != null && post.getAuthor().getUserId().equals(currentUserId))
                .build();
    }
}
