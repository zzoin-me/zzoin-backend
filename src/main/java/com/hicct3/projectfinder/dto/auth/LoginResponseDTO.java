package com.hicct3.projectfinder.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private Boolean recoveryRequired;
    private String recoveryToken;
    private LocalDateTime recoverableUntil;
}
