package com.hicct3.projectfinder.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                try (InputStream serviceAccount = resolveStream(serviceAccountPath)) {
                    if (serviceAccount == null) {
                        log.warn("Firebase credentials not found at {}. FCM disabled.", serviceAccountPath);
                        return;
                    }

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase Admin SDK initialized.");
                }
            }
        } catch (Exception e) {
            log.error("Firebase initialization failed: {}", e.getMessage());
        }
    }

    private InputStream resolveStream(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            return getClass().getClassLoader()
                    .getResourceAsStream(path.substring("classpath:".length()));
        }
        return new FileInputStream(path);
    }
}
