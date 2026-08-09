package com.hicct3.projectfinder.dto.community;

import com.hicct3.projectfinder.entity.Post;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostPreviewResponseDTO {
    private Long id;
    private String title;
    private String contentPreview;
    private AuthorDTO author;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Boolean likedByMe;
    private Boolean savedByMe;

    public static PostPreviewResponseDTO of(Post post, Integer likeCount, Integer commentCount, Boolean likedByMe, Boolean savedByMe) {
        String preview = post.getContent();
        if (preview != null && preview.length() > 120) {
            preview = preview.substring(0, 120);
        }
        return PostPreviewResponseDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .contentPreview(preview)
                .author(AuthorDTO.from(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .viewCount(post.getViewCount())
                .likedByMe(likedByMe)
                .savedByMe(savedByMe)
                .build();
    }
}
