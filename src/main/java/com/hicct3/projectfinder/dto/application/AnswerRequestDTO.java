package com.hicct3.projectfinder.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AnswerRequestDTO {
    @NotNull
    private Long questionId;

    @NotBlank
    @Size(max = 500, message = "답변은 500자 이하여야 합니다.")
    private String answerText;
}
