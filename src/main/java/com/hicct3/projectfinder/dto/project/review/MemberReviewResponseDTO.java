package com.hicct3.projectfinder.dto.project.review;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MemberReviewResponseDTO {
    private Long reviewId;
    private Long targetUserId;
    private String nickname;
    private List<String> recruitments;
    private String profileUrl;
    private Integer contribution;
    private Integer participation;
    private Integer responsibility;
    private Double avgRating;
    private String comment;
    private LocalDateTime createdAt;
    private Boolean hidden;
}
