package com.hicct3.projectfinder.dto.application;

import com.hicct3.projectfinder.entity.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateApplicantStatusDTO {
    @NotNull
    private ApplicationStatus status;

}