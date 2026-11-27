package com.example.uniride.backend.service;

import com.example.uniride.backend.model.NotificationType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Push notification payload sent as an FCM data-only message (docs/API.md §9).
 */
public record PushMessage(
        UUID notificationId,
        NotificationType type,
        UUID recipientUserId,
        String fcmToken,
        UUID tripId,
        UUID reservationId,
        String title,
        String body
) {
    public PushMessage {
        Objects.requireNonNull(notificationId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(recipientUserId);
        Objects.requireNonNull(fcmToken);
        Objects.requireNonNull(tripId);
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
    }

    /** Exact data map documented in docs/API.md §9. */
    public Map<String, String> toDataPayload() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type.name());
        data.put("notificationId", notificationId.toString());
        data.put("tripId", tripId.toString());
        data.put("reservationId", reservationId.toString());
        data.put("title", title);
        data.put("body", body);
        return Map.copyOf(data);
    }
}
