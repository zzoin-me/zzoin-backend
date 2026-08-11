package com.hicct3.projectfinder.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileImageUploadResponseDTO {
    private String profileUrl;

    public static ProfileImageUploadResponseDTO of(String profileUrl) {
        return ProfileImageUploadResponseDTO.builder()
                .profileUrl(profileUrl)
                .build();
    }
}
