package com.hicct3.projectfinder.dto.application;

import com.hicct3.projectfinder.dto.project.RecruitmentDetailResponseDTO;
import com.hicct3.projectfinder.entity.Project;
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
public class ProjectApplicantsResponseDTO {
    private List<ProjectApplicantResponseDTO> applicants;

    public static ProjectApplicantsResponseDTO of(List<ProjectApplicantResponseDTO> applicants) {
        return ProjectApplicantsResponseDTO.builder().applicants(applicants).build();
    }
}