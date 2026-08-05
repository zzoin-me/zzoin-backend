package com.hicct3.projectfinder.dto.project.review;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateMemberReviewRequestDTO {
    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 200)
    private String comment;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer contribution;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer participation;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer responsibility;
}