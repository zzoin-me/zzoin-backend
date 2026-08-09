package com.hicct3.projectfinder.event;

import com.hicct3.projectfinder.entity.enums.NotificationType;

public record ApplicationNotificationEvent(
        Long recipientId,
        NotificationType type,
        String title,
        String content,
        String targetUrl,
        Long applicationId
) {
}
