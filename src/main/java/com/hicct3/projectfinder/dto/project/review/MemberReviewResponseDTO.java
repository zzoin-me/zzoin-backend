package com.hicct3.projectfinder.dto.project.review;

import com.hicct3.projectfinder.entity.MemberReview;
import com.hicct3.projectfinder.entity.ProjectMember;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MemberReviewResponseDTO {
    private Long memberId;
    private String nickname;
    private List<String> recruitments;
    private String profileUrl;
    private Integer contribution;
    private Integer participation;
    private Integer responsibility;
}