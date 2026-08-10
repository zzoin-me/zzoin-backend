package com.hicct3.projectfinder.dto.project.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedReviewResponseDTO {
    private Long reviewId;
    private Long projectId;
    private String projectTitle;
    private Integer contribution;
    private Integer participation;
    private Integer responsibility;
    private Double avgRating;
    private String comment;
    private LocalDateTime createdAt;
}
