package com.hicct3.projectfinder.dto.community;

import com.hicct3.projectfinder.entity.User;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorDTO {
    private Long userId;
    private String nickname;
    private String profileUrl;

    public static AuthorDTO from(User user) {
        if (user == null) return null;
        return AuthorDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickName())
                .profileUrl(user.getProfileUrl())
                .build();
    }
}
