package com.hicct3.projectfinder.dto.project;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateRecruitmentRequestDTO {
    @NotBlank
    @Size(min = 2, max = 30, message = "모집 이름은 2자 이상 30자 이하여야 합니다.")
    private String name;

    @NotBlank
    @Min(0)
    @Max(100)
    private Integer count;

    @NotBlank
    @Size(min = 2, max = 200, message = "자격 요건은 2자 이상 200자 이하여야 합니다.")
    private String qualification;

    @NotBlank
    @Size(min = 2, max = 200, message = "선호 요건은 2자 이상 200자 이하여야 합니다.")
    private String preferred;
}