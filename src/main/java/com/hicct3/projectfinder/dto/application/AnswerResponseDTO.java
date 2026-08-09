package com.hicct3.projectfinder.dto.application;

import com.hicct3.projectfinder.entity.ApplicationAnswer;
import com.hicct3.projectfinder.entity.enums.QuestionType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AnswerResponseDTO {
    private String questionLabel;
    private QuestionType questionType;
    private String answerText;

    public static AnswerResponseDTO from(ApplicationAnswer answer) {
        return AnswerResponseDTO.builder()
                .questionLabel(answer.getQuestion().getLabel())
                .questionType(answer.getQuestion().getType())
                .answerText(answer.getAnswerText())
                .build();
    }
}
