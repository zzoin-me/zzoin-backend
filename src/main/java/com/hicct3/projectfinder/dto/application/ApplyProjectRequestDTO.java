package com.hicct3.projectfinder.dto.application;

import jakarta.validation.Valid;
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
public class ApplyProjectRequestDTO {
    @NotNull
    private Long recruitmentId;

    @NotBlank
    @Size(min = 10, max = 500, message = "자기소개서는 10자 이상 500자 이하여야 합니다.")
    private String letter;

    @Size(max = 10, message = "답변은 최대 10개까지 등록할 수 있습니다.")
    private List<@NotNull @Valid AnswerRequestDTO> answers;

}
