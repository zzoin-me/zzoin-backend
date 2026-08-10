package com.hicct3.projectfinder.dto.project.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTargetsResponseDTO {
    private Long projectId;
    private String projectTitle;
    private Long totalTargetCount;
    private Long reviewedTargetCount;
    private List<ReviewTargetResponseDTO> targets;
}
