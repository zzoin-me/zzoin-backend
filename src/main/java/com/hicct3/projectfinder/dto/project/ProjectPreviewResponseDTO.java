package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectRecruitment;
import com.hicct3.projectfinder.entity.enums.CollaborationType;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProjectPreviewResponseDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime recruitmentDeadline;
    private List<String> recruitments;
    private ProjectStatus status;
    private int currentCount;
    private int totalCount;
    private String imageUrl;


    public static ProjectPreviewResponseDTO from(Project project, List<ProjectRecruitment> recruitments) {
        return ProjectPreviewResponseDTO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .recruitments(recruitments.stream().map(ProjectRecruitment::getName).toList())
                .currentCount(recruitments.stream().mapToInt(ProjectRecruitment::getCurrentCount).sum())
                .totalCount(recruitments.stream().mapToInt(ProjectRecruitment::getRecruitmentCount).sum())
                .imageUrl(project.getImageUrl())
                .status(project.getStatus())
                .build();
    }
}