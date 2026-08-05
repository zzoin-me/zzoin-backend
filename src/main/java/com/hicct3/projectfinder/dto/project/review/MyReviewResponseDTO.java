package com.hicct3.projectfinder.dto.project.review;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MyReviewResponseDTO {
    private String projectName;
    private String comment;
    private Integer contribution;
    private Integer participation;
    private Integer responsibility;
    private Double avgRating;
    private LocalDateTime createdAt;
}