package com.hicct3.projectfinder.dto.community;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostImageUploadResponseDTO {
    private List<String> imageUrls;

    public static PostImageUploadResponseDTO of(List<String> imageUrls) {
        return PostImageUploadResponseDTO.builder()
                .imageUrls(imageUrls)
                .build();
    }
}
