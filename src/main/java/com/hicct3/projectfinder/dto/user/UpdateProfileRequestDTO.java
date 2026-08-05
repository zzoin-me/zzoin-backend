package com.hicct3.projectfinder.dto.user;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateProfileRequestDTO {
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
    private String nickName;

    @Size(max = 500, message = "자기소개는 500자 이하여야 합니다.")
    private String bio;

    private List<String> fields;

    private String profileUrl;

    private List<Long> stackIds;
}