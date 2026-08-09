package com.hicct3.projectfinder.dto.jobrole;

import com.hicct3.projectfinder.entity.JobCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class JobCategoryResponseDTO {
    private Long id;
    private String categoryCode;
    private String name;

    public static JobCategoryResponseDTO of(JobCategory jobCategory) {
        return JobCategoryResponseDTO.builder()
                .id(jobCategory.getId())
                .categoryCode(jobCategory.getCategoryCode().name())
                .name(jobCategory.getName())
                .build();
    }
}
