package com.hicct3.projectfinder.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePostRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    private String content;
}
