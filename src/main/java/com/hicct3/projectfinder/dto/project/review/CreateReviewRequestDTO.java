package com.hicct3.projectfinder.dto.project.review;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateReviewRequestDTO {
    @NotNull
    private Long targetUserId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer contribution;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer participation;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer responsibility;

    @Size(max = 200)
    private String comment;
}
