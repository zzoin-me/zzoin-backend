package com.hicct3.projectfinder.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendChatMessageRequestDTO {
    @NotBlank(message = "메시지를 입력해주세요.")
    @Size(max = 1000, message = "메시지는 최대 1000자까지 입력할 수 있습니다.")
    private String content;
}
