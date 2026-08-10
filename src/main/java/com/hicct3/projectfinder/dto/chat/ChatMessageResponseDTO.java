package com.hicct3.projectfinder.dto.chat;

import com.hicct3.projectfinder.entity.ProjectChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponseDTO {
    private Long id;
    private Long senderId;
    private String senderNickname;
    private String senderProfileUrl;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;

    public static ChatMessageResponseDTO from(ProjectChatMessage message, Long viewerId) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .senderId(message.getSender().getUserId())
                .senderNickname(message.getSender().getNickName())
                .senderProfileUrl(message.getSender().getProfileUrl())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .mine(message.getSender().getUserId().equals(viewerId))
                .build();
    }
}
