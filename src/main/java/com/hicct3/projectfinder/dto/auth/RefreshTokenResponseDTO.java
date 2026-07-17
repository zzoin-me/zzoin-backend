package com.hicct3.projectfinder.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class RefreshTokenResponseDTO {
    private String accessToken;
    private String refreshToken;
}
