package com.hicct3.projectfinder.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountRecoveryRequestDTO {
    private String recoveryToken;
}
