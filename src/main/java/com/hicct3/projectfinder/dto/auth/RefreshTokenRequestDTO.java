package com.hicct3.projectfinder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RefreshTokenRequestDTO {
    @NotBlank
    private String refreshToken;
}
