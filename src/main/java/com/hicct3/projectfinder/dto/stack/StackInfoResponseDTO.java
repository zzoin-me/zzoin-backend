package com.hicct3.projectfinder.dto.stack;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class StackInfoResponseDTO {
    private Long id;
    private String name;
}
