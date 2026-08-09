package com.hicct3.projectfinder.dto.auth;

import com.hicct3.projectfinder.global.oauth.OAuth2Attributes;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkAccountRequestDTO {
    @NotBlank
    private String tempToken;

    @NotBlank
    private String password;

    private String provider;
    private String providerId;
}
