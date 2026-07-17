package com.hicct3.projectfinder.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateSchoolProfileRequestDTO {
    @Size(min = 2, max = 20, message = "전공은 2자 이상 20자 이하여야 합니다.")
    private String major;

    @Min(1)
    @Max(5)
    private Integer grade;
}