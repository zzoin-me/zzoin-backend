package com.hicct3.projectfinder.dto.project;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateRecruitmentRequestDTO {
    //요청에 id없으면 새로 추가
    private Long recruitmentId;

    @Size(min = 2, max = 30, message = "모집 이름은 2자 이상 30자 이하여야 합니다.")
    private String name;

    @Min(0)
    @Max(100)
    private Integer count;

    @Size(min = 2, max = 200, message = "자격 요건은 2자 이상 200자 이하여야 합니다.")
    private String qualification;

    @Size(min = 2, max = 200, message = "선호 요건은 2자 이상 200자 이하여야 합니다.")
    private String preferred;
}