package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateQuestionRequestDTO {
    @NotNull
    private QuestionType type;

    @NotBlank
    @Size(max = 100, message = "질문은 100자 이하여야 합니다.")
    private String label;

    private List<@Size(max = 100, message = "질문 선택지는 각각 100자 이하여야 합니다.") String> options;

    @NotNull
    private Boolean required;
}
