package com.hicct3.projectfinder.dto.application;

import com.hicct3.projectfinder.entity.ProjectApplication;
import com.hicct3.projectfinder.entity.ProjectMember;
import com.hicct3.projectfinder.entity.Stack;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.MemberStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProjectMemberResponseDTO {
    private Long projectId;
    private String projectName;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;

    public static ProjectMemberResponseDTO from(ProjectMember member)
    {
        return ProjectMemberResponseDTO.builder()
                .projectId(member.getProject().getId())
                .projectName(member.getProject().getTitle())
                .joinedAt(member.getJoinedAt())
                .completedAt(member.getCompletedAt())
                .build();
    }
}