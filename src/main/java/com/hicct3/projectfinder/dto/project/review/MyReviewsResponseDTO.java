package com.hicct3.projectfinder.dto.project.review;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MyReviewsResponseDTO {
    private Double ratingAvg;
    private Integer score5;
    private Integer score4;
    private Integer score3;
    private Integer score2;
    private Integer score1;
    private List<MyReviewResponseDTO> reviews;
}