package com.hicct3.projectfinder.dto.univ;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class UnivInfoResponseDTO {
    private Long id;
    private String name;
}
