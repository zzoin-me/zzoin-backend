package com.hicct3.projectfinder.dto.community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentPageResponseDTO {
    private List<CommentResponseDTO> comments;
    private Long nextCursor;
    private Boolean hasNext;
}
