package com.hicct3.projectfinder.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class SocialSignUpRequestDTO {
    @NotBlank
    private String signupToken;

    @NotBlank
    @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9.]+$",
            message = "닉네임은 한글, 영문, 숫자, 점만 사용할 수 있습니다."
    )
    private String nickName;
}
