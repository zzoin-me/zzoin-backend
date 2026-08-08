package com.hicct3.projectfinder.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass()
                        .getClassLoader()
                        .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount == null) {
                    log.warn("firebase-service-account.json not found. FCM push will be disabled.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized.");
            }
        } catch (Exception e) {
            log.error("Firebase initialization failed: {}", e.getMessage());
        }
    }
}
