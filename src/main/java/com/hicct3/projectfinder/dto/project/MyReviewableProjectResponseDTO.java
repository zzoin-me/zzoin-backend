package com.hicct3.projectfinder.dto.project;

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
public class MyReviewableProjectResponseDTO {
    private Long projectId;
    private String title;
    private String recruitment;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
    private Boolean reviewCompleted;
}
