package com.hicct3.projectfinder.dto.chat;

import com.hicct3.projectfinder.entity.enums.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponseDTO {
    private Long projectId;
    private String projectTitle;
    private String projectImageUrl;
    private ProjectStatus projectStatus;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
