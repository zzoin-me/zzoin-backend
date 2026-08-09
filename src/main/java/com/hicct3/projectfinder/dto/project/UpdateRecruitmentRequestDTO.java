package com.hicct3.projectfinder.dto.project;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateRecruitmentRequestDTO implements RecruitmentRequest {
    private Long recruitmentId;

    @NotNull
    private Long jobRoleId;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer recruitmentCount;

    @NotBlank
    @Size(min = 2, max = 200, message = "자격 요건은 2자 이상 200자 이하여야 합니다.")
    private String qualification;

    @NotBlank
    @Size(min = 2, max = 200, message = "선호 요건은 2자 이상 200자 이하여야 합니다.")
    private String preferred;

}
