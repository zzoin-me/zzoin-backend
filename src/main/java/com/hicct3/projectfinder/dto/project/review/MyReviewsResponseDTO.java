package com.hicct3.projectfinder.dto.project.review;

import com.hicct3.projectfinder.dto.common.PageResponseDTO;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MyReviewsResponseDTO {
    private Double ratingAvg;
    private Integer ratingCount;
    private Double contributionAvg;
    private Double participationAvg;
    private Double responsibilityAvg;
    private Integer score5;
    private Integer score4;
    private Integer score3;
    private Integer score2;
    private Integer score1;
    private PageResponseDTO<ReceivedReviewResponseDTO> reviews;
}
