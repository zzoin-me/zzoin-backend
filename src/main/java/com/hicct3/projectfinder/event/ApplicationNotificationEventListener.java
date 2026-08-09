package com.hicct3.projectfinder.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationNotificationEventListener {

    private final ApplicationNotificationProcessor notificationProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApplicationNotificationEvent event) {
        try {
            notificationProcessor.process(event);
        } catch (Exception e) {
            log.error(
                    "Failed to create application notification: applicationId={}, recipientId={}",
                    event.applicationId(),
                    event.recipientId(),
                    e
            );
        }
    }
}
