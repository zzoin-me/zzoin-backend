package com.hicct3.projectfinder.dto.stack.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CreateStackRequestDTO {
    @NotBlank
    private String name;
}
