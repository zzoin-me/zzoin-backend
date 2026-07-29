package com.hicct3.projectfinder.dto.project.review;

import com.hicct3.projectfinder.entity.Project;
import com.hicct3.projectfinder.entity.ProjectMember;
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
public class MemberResponseDTO {
    private Long userId;
    private String nickname;
    private String recruitment;
    private String profileUrl;

    public static MemberResponseDTO from(ProjectMember member) {
        return MemberResponseDTO.builder()
                .userId(member.getUser().getUserId())
                .nickname(member.getUser().getNickName())
                .recruitment(member.getJobName())
                .profileUrl(member.getUser().getProfileUrl())
                .build();
    }
}