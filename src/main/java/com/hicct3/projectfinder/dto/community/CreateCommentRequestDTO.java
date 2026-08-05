package com.hicct3.projectfinder.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentRequestDTO {
    @NotBlank
    @Size(max = 1000)
    private String content;

    private Long parentId;
}
