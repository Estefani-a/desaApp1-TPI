package com.example.uniride.presentation.navigation

import com.example.uniride.domain.model.AppMode
import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.model.TripId
import com.example.uniride.presentation.reservations.DriverPanelRoute
import com.example.uniride.presentation.reservations.MyTripRoute
import java.util.UUID

/** Screen that a push opens when it is tapped (docs/API.md §9). */
sealed interface PushDestination {

    /** Mode the screen belongs to. The shell switches to it if the profile allows it. */
    val mode: AppMode

    /** C4 Panel del conductor, for a new request on one of their trips. */
    data class DriverPanel(val tripId: TripId) : PushDestination {
        override val mode get() = AppMode.CONDUCTOR
    }

    /** B6 Mi viaje, for the answer to the passenger's request. */
    data class MyTrip(val tripId: TripId) : PushDestination {
        override val mode get() = AppMode.PASAJERO
    }

    companion object {
        fun of(type: NotificationType, tripId: TripId): PushDestination = when (type) {
            NotificationType.NUEVA_SOLICITUD -> DriverPanel(tripId)
            NotificationType.SOLICITUD_ACEPTADA, NotificationType.SOLICITUD_RECHAZADA -> MyTrip(tripId)
        }

        /**
         * Reads the data of the push as it arrives in the intent extras. Anything that is not a
         * known type with a trip id is ignored: the activity is exported, so any app can start it
         * with extras.
         */
        fun fromExtras(type: String?, tripId: String?): PushDestination? {
            val notificationType = NotificationType.entries.firstOrNull { it.name == type } ?: return null
            val uuid = tripId?.let(::canonicalUuid) ?: return null
            return of(notificationType, TripId(uuid))
        }

        /** The id as the server and Room store it (lowercase), or `null` if it is not a UUID. */
        private fun canonicalUuid(value: String): String? =
            runCatching { UUID.fromString(value).toString() }.getOrNull()?.takeIf { it == value.lowercase() }
    }
}

/** Typed route of the screen. */
internal val PushDestination.route: Any
    get() = when (this) {
        is PushDestination.DriverPanel -> DriverPanelRoute(tripId.value)
        is PushDestination.MyTrip -> MyTripRoute(tripId.value)
    }
