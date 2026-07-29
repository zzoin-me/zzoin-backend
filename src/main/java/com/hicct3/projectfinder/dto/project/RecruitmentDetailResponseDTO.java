package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RecruitmentDetailResponseDTO {
    private Long id;
    private String name;
    private RecruitmentCategory category;
    private Integer applicantCount;
    private Integer recruitmentCount;
    private String qualification;
    private String preferred;

    public static RecruitmentDetailResponseDTO from(ProjectRecruitment projectRecruitment)
    {
        return RecruitmentDetailResponseDTO.builder()
                .id(projectRecruitment.getId())
                .name(projectRecruitment.getName())
                .category(projectRecruitment.getCategory())
                .applicantCount(projectRecruitment.getApplicantCount())
                .recruitmentCount(projectRecruitment.getRecruitmentCount())
                .qualification(projectRecruitment.getQualification())
                .preferred(projectRecruitment.getPreferred())
                .build();
    }
}