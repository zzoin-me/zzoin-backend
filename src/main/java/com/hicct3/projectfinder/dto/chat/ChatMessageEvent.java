package com.hicct3.projectfinder.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageEvent(
        Long id,
        Long projectId,
        Long senderId,
        String senderNickname,
        String senderProfileUrl,
        String content,
        LocalDateTime createdAt
) {
    public ChatMessageResponseDTO forViewer(Long viewerId) {
        return ChatMessageResponseDTO.builder()
                .id(id)
                .senderId(senderId)
                .senderNickname(senderNickname)
                .senderProfileUrl(senderProfileUrl)
                .content(content)
                .createdAt(createdAt)
                .mine(senderId.equals(viewerId))
                .build();
    }
}
