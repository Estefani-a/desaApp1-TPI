package com.example.uniride.data.push

import com.example.uniride.domain.model.NotificationType
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.SessionRepository
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates what the app does upon receiving an FCM data message (docs/API.md §9):
 * 1. Ignores malformed payloads or pushes received while logged out.
 * 2. Displays the notification right away (so slow networks never delay the alert).
 * 3. Refreshes Room in the background (`refreshTripReservations` for `NUEVA_SOLICITUD`;
 *    `refreshTrip` for `SOLICITUD_ACEPTADA` and `SOLICITUD_RECHAZADA` so accepted trips bring
 *    plate and phone).
 */
@Singleton
class PushMessageHandler @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val notifier: PushNotifier,
) {
    suspend fun handleMessage(data: Map<String, String>) {
        val payload = PushPayload.fromData(data) ?: return
        if (!sessionRepository.observeIsLoggedIn().first()) return

        notifier.show(payload)

        when (payload.type) {
            NotificationType.NUEVA_SOLICITUD -> {
                reservationRepository.refreshTripReservations(payload.tripId)
            }
            NotificationType.SOLICITUD_ACEPTADA,
            NotificationType.SOLICITUD_RECHAZADA,
            -> {
                tripRepository.refreshTrip(payload.tripId)
            }
        }
    }
}
