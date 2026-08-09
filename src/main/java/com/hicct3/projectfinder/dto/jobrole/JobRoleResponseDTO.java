package com.hicct3.projectfinder.dto.jobrole;

import com.hicct3.projectfinder.entity.JobRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class JobRoleResponseDTO {
    private Long id;
    private String name;
    private Long jobCategoryId;

    public static JobRoleResponseDTO of(JobRole jobRole) {
        return JobRoleResponseDTO.builder()
                .id(jobRole.getId())
                .name(jobRole.getName())
                .jobCategoryId(jobRole.getJobCategory() != null ? jobRole.getJobCategory().getId() : null)
                .build();
    }
}
