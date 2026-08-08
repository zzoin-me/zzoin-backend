package com.hicct3.projectfinder.dto.project.myproject;

import com.hicct3.projectfinder.entity.ProjectApplication;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.JobCategoryCode;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MyApplicationPreviewResponseDTO {
    private Long applicationId;
    private Long projectId;
    private String projectTitle;
    private String appliedRecruitmentName;
    private JobCategoryCode appliedRecruitmentCategory;
    private ApplicationStatus status;
    private LocalDateTime createdAt;


    public static MyApplicationPreviewResponseDTO from(ProjectApplication application) {
        return MyApplicationPreviewResponseDTO.builder()
                .applicationId(application.getId())
                .projectId(application.getRecruitment().getProject().getId())
                .projectTitle(application.getRecruitment().getProject().getTitle())
                .appliedRecruitmentCategory(application.getRecruitment().getJobRole().getJobCategory().getCategoryCode())
                .appliedRecruitmentName(application.getRecruitment().getJobRole().getName())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
}