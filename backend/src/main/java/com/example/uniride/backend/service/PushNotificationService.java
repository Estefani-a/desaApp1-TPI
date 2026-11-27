package com.example.uniride.backend.service;

import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Listens for committed RF04 reservation events and dispatches FCM push notifications
 * (docs/API.md §9). If the recipient has no registered device token (RNF08), nothing is sent.
 * If FCM reports an unregistered token, it is cleared from the user record.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PushGateway pushGateway;

    public PushNotificationService(
            UserRepository userRepository,
            UserService userService,
            PushGateway pushGateway
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.pushGateway = pushGateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushNotification(PushNotificationEvent event) {
        User recipient = userRepository.findById(event.recipientUserId()).orElse(null);
        if (recipient == null) {
            return;
        }
        String fcmToken = recipient.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug(
                    "Omitiendo push {} para userId={}: sin token FCM registrado.",
                    event.type(),
                    event.recipientUserId()
            );
            return;
        }

        PushMessage message = new PushMessage(
                UUID.randomUUID(),
                event.type(),
                event.recipientUserId(),
                fcmToken,
                event.tripId(),
                event.reservationId(),
                event.title(),
                event.body()
        );

        PushGateway.SendResult result = pushGateway.send(message);
        if (result == PushGateway.SendResult.UNREGISTERED_TOKEN) {
            userService.clearStaleFcmToken(event.recipientUserId(), fcmToken);
        }
    }
}
