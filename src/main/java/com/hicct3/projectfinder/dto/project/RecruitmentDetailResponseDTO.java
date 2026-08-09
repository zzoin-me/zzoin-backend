package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RecruitmentDetailResponseDTO {
    private Long id;
    private Long jobRoleId;
    private String name;
    private JobCategoryCode category;
    private Integer applicantCount;
    private Integer recruitmentCount;
    private String qualification;
    private String preferred;

    public static RecruitmentDetailResponseDTO from(ProjectRecruitment projectRecruitment)
    {
        return RecruitmentDetailResponseDTO.builder()
                .id(projectRecruitment.getId())
                .jobRoleId(projectRecruitment.getJobRole().getId())
                .category(projectRecruitment.getJobRole().getJobCategory().getCategoryCode())
                .name(projectRecruitment.getJobRole().getName())
                .applicantCount(projectRecruitment.getApplicantCount())
                .recruitmentCount(projectRecruitment.getRecruitmentCount())
                .qualification(projectRecruitment.getQualification())
                .preferred(projectRecruitment.getPreferred())
                .build();
    }
}
