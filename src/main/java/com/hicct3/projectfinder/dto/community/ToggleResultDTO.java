package com.hicct3.projectfinder.dto.community;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToggleResultDTO {
    private Boolean active;

    public static ToggleResultDTO of(Boolean active) {
        return ToggleResultDTO.builder().active(active).build();
    }
}
