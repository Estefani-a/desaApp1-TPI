package com.example.uniride.backend.service;

/**
 * Delivery port for FCM data-only push notifications (docs/API.md §9).
 * Implemented by {@link FcmPushGateway} when credentials are configured and by
 * {@link LoggingPushGateway} otherwise.
 */
public interface PushGateway {

    SendResult send(PushMessage message);

    enum SendResult {
        SENT,
        LOGGED_ONLY,
        UNREGISTERED_TOKEN,
        FAILED
    }
}
