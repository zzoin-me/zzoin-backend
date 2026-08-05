package com.hicct3.projectfinder.dto.application;

import com.hicct3.projectfinder.entity.*;
import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import com.hicct3.projectfinder.entity.enums.RecruitmentCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ProjectApplicantResponseDTO {
    private Long applicationId;
    private Long userId;
    private String nickName;
    private String profileUrl;
    private String recruitmentName;
    private RecruitmentCategory recruitmentCategory;
    private List<String> stackNames;
    private LocalDateTime applicationDate;
    private String letter;
    private String schoolName;
    private String major;
    private Integer grade;
    private Double ratingAvg;
    private ApplicationStatus status;

    private List<ProjectMemberResponseDTO> histories;

    private List<AnswerResponseDTO> answers;

    public static ProjectApplicantResponseDTO from(ProjectApplication application, List<ProjectMember> members, List<AnswerResponseDTO> answers)
    {
        return ProjectApplicantResponseDTO.builder()
                .applicationId(application.getId())
                .userId(application.getUser().getUserId())
                .nickName(application.getUser().getNickName())
                .profileUrl(application.getUser().getProfileUrl())
                .recruitmentName(application.getRecruitment().getName())
                .recruitmentCategory(application.getRecruitment().getCategory())
                .stackNames(application.getUser().getStacks().stream().map(Stack::getName).toList())
                .applicationDate(application.getCreatedAt())
                .letter(application.getLetter())
                .schoolName(application.getUser().getSchoolDomain().getName())
                .major(application.getUser().getMajor())
                .grade(application.getUser().getGrade())
                .ratingAvg(application.getUser().getRatingAvg())
                .status(application.getStatus())
                .histories(members.stream().map(ProjectMemberResponseDTO::from).toList())
                .answers(answers)
                .build();
    }
}