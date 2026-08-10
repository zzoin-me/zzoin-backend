package com.hicct3.projectfinder.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.hicct3.projectfinder.entity.DeviceToken;
import com.hicct3.projectfinder.entity.User;
import com.hicct3.projectfinder.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Async
    public void sendToUser(Long userId, String title, String body, String targetUrl) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.debug("Firebase not initialized, skipping FCM push");
                return;
            }

            List<DeviceToken> tokens = deviceTokenRepository.findAllByUser_UserId(userId);

            if (tokens.isEmpty()) {
                log.debug("No device tokens for user {}", userId);
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("title", title);
            data.put("body", body);
            if (targetUrl != null) {
                data.put("targetUrl", targetUrl);
            }

            List<String> tokenStrings = tokens.stream().map(DeviceToken::getToken).toList();

            MulticastMessage message = MulticastMessage.builder()
                    .putAllData(data)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(com.google.firebase.messaging.Aps.builder()
                                    .setSound("default")
                                    .setContentAvailable(true)
                                    .build())
                            .build())
                    .addAllTokens(tokenStrings)
                    .build();

            FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM push sent to user {} ({} tokens)", userId, tokenStrings.size());

        } catch (Exception e) {
            log.error("FCM push failed for user {}: {}", userId, e.getMessage());
        }
    }
}
