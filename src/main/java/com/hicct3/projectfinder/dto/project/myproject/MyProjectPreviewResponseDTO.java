package com.hicct3.projectfinder.dto.project.myproject;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MyProjectPreviewResponseDTO {
    private Long id;
    private String title;
    private ProjectStatus status;
    private Integer applicantCount;
    private LocalDateTime createdAt;


    public static MyProjectPreviewResponseDTO from(Project project, List<ProjectRecruitment> recruitments) {
        return MyProjectPreviewResponseDTO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .status(project.getStatus())
                .applicantCount(recruitments.stream().mapToInt(ProjectRecruitment::getApplicantCount).sum())
                .createdAt(project.getCreatedAt())
                .build();
    }
}