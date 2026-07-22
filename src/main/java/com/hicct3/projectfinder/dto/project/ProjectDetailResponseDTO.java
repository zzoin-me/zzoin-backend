package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.enums.CollaborationType;
import com.hicct3.projectfinder.entity.enums.GoalType;
import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProjectDetailResponseDTO {
    private Long id;
    private String title;
    private String description;
    private CollaborationType collaborationType;
    private String communicationTool;
    private String meetingSchedule;
    private String period;
    private LocalDateTime recruitmentDeadline;
    private GoalType goalType;
    private String imageUrl;
    private ProjectStatus projectStatus;
    private String authorNickname;
    private List<RecruitmentDetailResponseDTO> recruitments;

    public static ProjectDetailResponseDTO from(Project project, List<RecruitmentDetailResponseDTO> recruitments)
    {
        return ProjectDetailResponseDTO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .collaborationType(project.getCollaborationType())
                .communicationTool(project.getCommunicationTool())
                .meetingSchedule(project.getMeetingSchedule())
                .period(project.getPeriod())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .goalType(project.getGoal())
                .imageUrl(project.getImageUrl())
                .projectStatus(project.getStatus())
                .authorNickname(project.getAuthor().getNickName())
                .recruitments(recruitments)
                .build();
    }
}
