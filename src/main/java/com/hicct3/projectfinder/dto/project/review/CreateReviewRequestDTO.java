package com.hicct3.projectfinder.dto.project.review;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateReviewRequestDTO {
    @NotNull
    private List<CreateMemberReviewRequestDTO> members;
}