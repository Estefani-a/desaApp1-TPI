package com.example.uniride.backend.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends data-only FCM messages via Firebase Admin SDK (docs/API.md §9).
 * Never sets a notification block so the Android client handles display and navigation in
 * both foreground and background states.
 */
public class FcmPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushGateway(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public SendResult send(PushMessage message) {
        Message fcmMessage = Message.builder()
                .setToken(message.fcmToken())
                .putAllData(message.toDataPayload())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();
        try {
            String messageId = firebaseMessaging.send(fcmMessage);
            log.debug(
                    "Push FCM enviada: messageId={} type={} recipientUserId={} tripId={}",
                    messageId,
                    message.type(),
                    message.recipientUserId(),
                    message.tripId()
            );
            return SendResult.SENT;
        } catch (FirebaseMessagingException ex) {
            MessagingErrorCode errorCode = ex.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn(
                        "Token FCM inválido o desregistrado ({}) para recipientUserId={} type={}",
                        errorCode,
                        message.recipientUserId(),
                        message.type()
                );
                return SendResult.UNREGISTERED_TOKEN;
            }
            log.warn(
                    "Error al enviar push FCM ({}) para recipientUserId={} type={}: {}",
                    errorCode,
                    message.recipientUserId(),
                    message.type(),
                    ex.getMessage()
            );
            return SendResult.FAILED;
        }
    }
}
