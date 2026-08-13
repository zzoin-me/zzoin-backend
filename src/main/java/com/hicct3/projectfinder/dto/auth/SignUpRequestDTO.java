package com.hicct3.projectfinder.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class SignUpRequestDTO {
    @NotBlank
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9.]+$",
            message = "닉네임은 한글, 영문, 숫자, 점만 사용할 수 있습니다."
    )
    private String nickName;

    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 8자 이상이며 영문과 숫자를 모두 포함해야 합니다."
    )
    private String password;

    @NotBlank
    private String signupToken;
}
