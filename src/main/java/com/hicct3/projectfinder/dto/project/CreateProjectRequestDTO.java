package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.dto.project.recruitment.CreateRecruitmentRequestDTO;
import com.hicct3.projectfinder.entity.enums.CollaborationType;
import com.hicct3.projectfinder.entity.enums.GoalType;
import jakarta.validation.Valid;
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
public class CreateProjectRequestDTO {
    @NotBlank
    @Size(min = 2, max = 30, message = "제목은 2자 이상 30자 이하여야 합니다.")
    private String title;

    @NotBlank
    @Size(min = 2, max = 500, message = "내용은 2자 이상 500자 이하여야 합니다.")
    private String description;

    @NotNull
    private CollaborationType collaborationType;

    @NotBlank
    @Size(min = 2, max = 50, message = "커뮤니케이션 도구는 2자 이상 50자 이하여야 합니다.")
    private String communicationTool;

    @Size(min = 2, max = 50, message = "정기모임은 2자 이상 50자 이하여야 합니다.")
    private String meetingSchedule;

    @Size(min = 2, max = 20, message = "정기모임은 2자 이상 20자 이하여야 합니다.")
    private String period;

    @NotNull
    private LocalDateTime recruitmentDeadline;

    @NotNull
    private GoalType goalType;

    @NotBlank
    @Size(min = 2, max=255, message = "이미지는 2자 이상 255자 이하여야 합니다.")
    private String imageUrl;

    @NotNull
    @Size(min = 1, max = 6, message = "모집 역할은 1개 이상 6개 이하로 등록할 수 있습니다.")
    private List<@Valid CreateRecruitmentRequestDTO> recruitments;

    @Size(max = 10, message = "질문은 최대 10개까지 등록할 수 있습니다.")
    private List<@Valid CreateQuestionRequestDTO> questions;

}
