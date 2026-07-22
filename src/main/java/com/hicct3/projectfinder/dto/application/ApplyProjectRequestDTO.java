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
public class ApplyProjectRequestDTO {
    @NotNull
    private Long recruitmentId;

    @NotBlank
    @Size(min = 10, max = 500, message = "자기소개서는 10자 이상 500자 이하여야 합니다.")
    private String letter;

}