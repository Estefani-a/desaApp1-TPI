package com.example.uniride.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fallback push gateway used when {@code FIREBASE_CREDENTIALS_PATH} is empty.
 * Logs push metadata without printing the sensitive device token.
 */
public class LoggingPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    @Override
    public SendResult send(PushMessage message) {
        log.info(
                "[FCM-LOG] type={} recipientUserId={} tripId={} reservationId={} title=\"{}\"",
                message.type(),
                message.recipientUserId(),
                message.tripId(),
                message.reservationId(),
                message.title()
        );
        return SendResult.LOGGED_ONLY;
    }
}
