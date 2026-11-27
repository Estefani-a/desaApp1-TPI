package com.example.uniride.backend.config;

import com.example.uniride.backend.service.FcmPushGateway;
import com.example.uniride.backend.service.LoggingPushGateway;
import com.example.uniride.backend.service.PushGateway;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configures Firebase Admin SDK when {@code FIREBASE_CREDENTIALS_PATH} is provided, or falls back
 * to {@link LoggingPushGateway} so local Docker and CI environments run without credentials.
 */
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    private static final String APP_NAME = "uniride-fcm";

    @Bean
    public PushGateway pushGateway(FirebaseProperties properties) {
        if (!properties.hasCredentialsPath()) {
            log.info("FIREBASE_CREDENTIALS_PATH vacío: las notificaciones push FCM quedan en modo log.");
            return new LoggingPushGateway();
        }

        Path credentialsFile = Path.of(properties.credentialsPath().trim());
        if (!Files.isRegularFile(credentialsFile)) {
            throw new IllegalStateException(
                    "FIREBASE_CREDENTIALS_PATH apunta a un archivo inexistente: " + credentialsFile
            );
        }

        FirebaseApp app = getOrInitFirebaseApp(credentialsFile);
        log.info("Firebase Admin SDK inicializado para notificaciones push FCM.");
        return new FcmPushGateway(FirebaseMessaging.getInstance(app));
    }

    private FirebaseApp getOrInitFirebaseApp(Path credentialsFile) {
        for (FirebaseApp existing : FirebaseApp.getApps()) {
            if (APP_NAME.equals(existing.getName())) {
                return existing;
            }
        }
        try (InputStream stream = Files.newInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            return FirebaseApp.initializeApp(options, APP_NAME);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se pudieron leer las credenciales de Firebase desde " + credentialsFile, ex
            );
        }
    }
}
