package com.hicct3.projectfinder.dto.community;

import com.hicct3.projectfinder.entity.Comment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private AuthorDTO author;
    private Long parentId;
    private Integer depth;
    private LocalDateTime createdAt;
    private Boolean isMine;
    private Boolean isDeleted;
    @Builder.Default
    private List<CommentResponseDTO> children = new ArrayList<>();

    public static CommentResponseDTO of(Comment comment, Long currentUserId) {
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .author(comment.isDeleted() ? null : AuthorDTO.from(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .depth(comment.getDepth())
                .createdAt(comment.getCreatedAt())
                .isMine(comment.getAuthor() != null && comment.getAuthor().getUserId().equals(currentUserId))
                .isDeleted(comment.isDeleted())
                .build();
    }
}
