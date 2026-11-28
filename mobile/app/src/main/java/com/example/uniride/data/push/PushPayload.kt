package com.example.uniride.data.push

import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId
import java.util.UUID

/**
 * Validated payload of an incoming FCM data message (docs/API.md §9).
 *
 * Unknown types or malformed UUIDs are rejected by [fromData] so untrusted or future message types
 * never crash the client.
 */
data class PushPayload(
    val type: NotificationType,
    val tripId: TripId,
    val notificationId: String?,
    val title: String?,
    val body: String?,
) {
    /** Stable Android notification id so repeated deliveries update instead of stacking duplicates. */
    val notificationIntId: Int
        get() = (notificationId ?: "${type.name}:${tripId.value}").hashCode()

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_NOTIFICATION_ID = "notificationId"
        const val KEY_TRIP_ID = "tripId"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"

        fun fromData(data: Map<String, String>): PushPayload? {
            val rawType = data[KEY_TYPE]?.trim() ?: return null
            val type = NotificationType.entries.firstOrNull { it.name == rawType } ?: return null
            val canonicalTripId = data[KEY_TRIP_ID]?.trim()?.let(::canonicalUuid) ?: return null

            return PushPayload(
                type = type,
                tripId = TripId(canonicalTripId),
                notificationId = data[KEY_NOTIFICATION_ID]?.trim()?.takeIf { it.isNotEmpty() },
                title = data[KEY_TITLE]?.trim()?.takeIf { it.isNotEmpty() },
                body = data[KEY_BODY]?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        private fun canonicalUuid(value: String): String? =
            runCatching { UUID.fromString(value).toString() }
                .getOrNull()
                ?.takeIf { it == value.lowercase() }
    }
}
