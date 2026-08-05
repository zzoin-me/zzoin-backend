package com.hicct3.projectfinder.dto.project;

import com.hicct3.projectfinder.entity.ProjectQuestion;
import com.hicct3.projectfinder.entity.enums.QuestionType;
import lombok.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class QuestionResponseDTO {
    private Long id;
    private QuestionType type;
    private String label;
    private List<String> options;
    private Boolean required;
    private Integer orderIndex;

    public static QuestionResponseDTO from(ProjectQuestion question) {
        List<String> options = question.getOptions() != null && !question.getOptions().isBlank()
                ? Arrays.asList(question.getOptions().split(","))
                : Collections.emptyList();

        return QuestionResponseDTO.builder()
                .id(question.getId())
                .type(question.getType())
                .label(question.getLabel())
                .options(options)
                .required(question.getRequired())
                .orderIndex(question.getOrderIndex())
                .build();
    }
}
