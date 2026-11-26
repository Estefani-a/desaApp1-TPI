package com.example.uniride.domain.usecase.reservation

import com.example.uniride.domain.model.Reservation
import com.example.uniride.domain.model.ReservationRejection
import com.example.uniride.domain.model.ReservationStatus
import com.example.uniride.domain.model.Trip
import com.example.uniride.domain.model.TripId
import com.example.uniride.domain.repository.ReservationRepository
import com.example.uniride.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** A request as the driver panel (C4) shows it. */
data class DriverRequest(
    val reservation: Reservation,
    /** The answer was given on the phone and not sent yet. */
    val isPendingSync: Boolean,
    /** Why the server did not accept the answer, if it did not (for example, no seats left). */
    val rejection: ReservationRejection?,
) {
    val canBeAnswered: Boolean get() = reservation.canBeAnswered
}

/** The driver panel (C4) of one trip. */
data class DriverPanel(
    val trip: Trip,
    /** Pending first, then accepted, then the rest; oldest first inside each group. */
    val requests: List<DriverRequest>,
    val hasStarted: Boolean,
)

/**
 * The requests of a trip the user drives (RF04, C4), from the phone. `null` if the trip is not
 * on the phone. The requests come in with `ReservationRepository.refreshTripReservations`.
 */
class ObserveDriverPanelUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val reservationRepository: ReservationRepository,
    private val clock: Clock,
) {
    operator fun invoke(tripId: TripId): Flow<DriverPanel?> =
        combine(
            tripRepository.observeTrip(tripId),
            reservationRepository.observeTripReservations(tripId),
            reservationRepository.observePendingReservationIds(),
            reservationRepository.observeRejections(),
        ) { trip, reservations, pending, rejections ->
            trip ?: return@combine null
            DriverPanel(
                trip = trip,
                requests = reservations
                    .sortedWith(compareBy({ ORDER.indexOf(it.status) }, { it.reservedAt }))
                    .map { DriverRequest(it, it.id in pending, rejections[it.id]) },
                hasStarted = trip.schedule.hasDepartedAt(LocalDateTime.now(clock)),
            )
        }.distinctUntilChanged()

    private companion object {
        val ORDER = listOf(
            ReservationStatus.PENDIENTE,
            ReservationStatus.ACEPTADA,
            ReservationStatus.RECHAZADA,
            ReservationStatus.CANCELADA,
        )
    }
}
