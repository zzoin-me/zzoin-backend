package com.hicct3.projectfinder.dto.chat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatMessagesResponseDTO {
    private List<ChatMessageResponseDTO> messages;
    private Long nextCursor;
    private boolean hasNext;
}
