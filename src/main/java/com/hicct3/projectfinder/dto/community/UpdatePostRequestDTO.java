package com.hicct3.projectfinder.dto.community;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePostRequestDTO {
    @Size(max = 100)
    private String title;

    @Size(max = 10000)
    private String content;

    @Size(max = 10)
    private List<@Size(max = 2048) String> imageUrls;
}
