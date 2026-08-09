package com.hicct3.projectfinder.event;

import com.hicct3.projectfinder.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApplicationNotificationProcessor {

    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(ApplicationNotificationEvent event) {
        notificationService.createNotification(
                event.recipientId(),
                event.type(),
                event.title(),
                event.content(),
                event.targetUrl(),
                event.applicationId()
        );
    }
}
