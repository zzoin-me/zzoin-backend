package com.hicct3.projectfinder.dto.project.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetResponseDTO {
    private Long userId;
    private String nickname;
    private String recruitment;
    private String profileUrl;
    private Boolean reviewed;
}
