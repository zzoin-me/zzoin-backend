package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.ProjectRecruitment;
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
public class RecruitmentDetailResponseDTO {
    private Long id;
    private String name;
    private Integer applicantCount;
    private Integer recruitmentCount;
    private String qualification;
    private String preferred;

    public static RecruitmentDetailResponseDTO from(ProjectRecruitment projectRecruitment)
    {
        return RecruitmentDetailResponseDTO.builder()
                .id(projectRecruitment.getId())
                .name(projectRecruitment.getName())
                .applicantCount(projectRecruitment.getApplicantCount())
                .recruitmentCount(projectRecruitment.getRecruitmentCount())
                .qualification(projectRecruitment.getQualification())
                .preferred(projectRecruitment.getPreferred())
                .build();
    }
}