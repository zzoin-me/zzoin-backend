package com.hicct3.projectfinder.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UnivEmailVerifyRequestDTO {
    @NotBlank
    @Email
    private String verifyEmail;
    @NotBlank
    private String code;

    @NotNull
    private Long univId;
}
