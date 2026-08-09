package com.hicct3.projectfinder.dto.community;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePostRequestDTO {
    @Size(max = 100)
    private String title;

    private String content;
}
