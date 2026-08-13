package com.hicct3.projectfinder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UnlinkSocialRequestDTO {
    @NotBlank
    private String password;
}
