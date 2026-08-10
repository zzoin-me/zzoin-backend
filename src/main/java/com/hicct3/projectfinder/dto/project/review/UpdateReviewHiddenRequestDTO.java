package com.hicct3.projectfinder.dto.project.review;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewHiddenRequestDTO {
    @NotNull
    private Boolean hidden;
}
