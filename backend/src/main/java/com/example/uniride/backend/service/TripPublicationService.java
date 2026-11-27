package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.PublishTripRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Campus;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripRoute;
import com.example.uniride.backend.model.TripSchedule;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.CampusRepository;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.repository.UserRepository;
import com.example.uniride.backend.util.BusinessTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * What a driver does with their trips (RF02, docs/API.md §5.4 and §5.6) and "Mis viajes"
 * (§5.5). The transitions of the status live in {@link Trip} and {@link Reservation}.
 */
@Service
public class TripPublicationService {

    /** docs/API.md §5.5: finished and cancelled trips are listed during this many days. */
    static final int CLOSED_TRIPS_DAYS = 7;

    /** Which reservation of a passenger on a trip the app shows: the active one, else the newest. */
    private static final Comparator<Reservation> MOST_RELEVANT = Comparator
            .comparing((Reservation reservation) -> reservation.getStatus().isActive())
            .thenComparing(Reservation::getReservedAt);

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final ViewMapper viewMapper;
    private final Clock clock;

    public TripPublicationService(TripRepository tripRepository, ReservationRepository reservationRepository,
                                  UserRepository userRepository, CampusRepository campusRepository,
                                  ViewMapper viewMapper, Clock clock) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.campusRepository = campusRepository;
        this.viewMapper = viewMapper;
        this.clock = clock;
    }

    /**
     * {@code PUT /trips/{tripId}}, idempotent: if the trip of that id is already of this driver
     * it is returned as it is, even if the body would not be valid anymore (a retry that arrives
     * after the departure). The lock on the driver makes simultaneous retries wait their turn.
     *
     * @throws ApiException {@code ID_CONFLICT} if the id is of another user's trip;
     *                      {@code VALIDATION_ERROR} if the new trip breaks a rule of §5.4
     */
    @Transactional
    public PublishResult publish(UUID userId, UUID tripId, PublishTripRequestDTO request) {
        User driver = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

        Optional<Trip> existing = tripRepository.findById(tripId);
        if (existing.isPresent()) {
            if (!existing.get().isDrivenBy(userId)) {
                throw new ApiException(ErrorCode.ID_CONFLICT);
            }
            return new PublishResult(viewMapper.trip(existing.get(), userId, null), false);
        }

        Campus campus = validate(driver, request);
        Trip trip = tripRepository.save(new Trip(tripId, driver, campus,
                new TripRoute(request.direction(), request.meetingPoint(),
                        request.destinationNeighborhood().strip(), blankToNull(request.destinationReference())),
                new TripSchedule(request.departureDate(), request.departureTime(), request.estimatedArrivalTime()),
                request.totalSeats(), request.contributionPerSeat(), clock.instant()));
        return new PublishResult(viewMapper.trip(trip, userId, null), true);
    }

    /** {@code POST /trips/{tripId}/finish}: only the driver. Reservations stay as they are. */
    @Transactional
    public TripResponseDTO finish(UUID userId, UUID tripId) {
        Trip trip = lockOwnTrip(userId, tripId);
        trip.finish(clock.instant());
        return viewMapper.trip(trip, userId, null);
    }

    /**
     * {@code POST /trips/{tripId}/cancel}: only the driver. The pending and accepted
     * reservations are cancelled in the same transaction. No push is sent (§5.6).
     */
    @Transactional
    public TripResponseDTO cancel(UUID userId, UUID tripId) {
        Trip trip = lockOwnTrip(userId, tripId);
        if (trip.cancel(clock.instant())) {
            reservationRepository.findByTripIdAndStatusIn(tripId, ReservationStatus.ACTIVE)
                    .forEach(reservation -> reservation.cancelWithTrip(clock.instant()));
        }
        return viewMapper.trip(trip, userId, null);
    }

    /**
     * {@code GET /users/me/trips}: the trips the user drives or has a reservation on, by
     * departure. Open and full ones always; finished and cancelled ones only if they left in the
     * last {@value #CLOSED_TRIPS_DAYS} days or are still to come.
     */
    @Transactional(readOnly = true)
    public List<TripResponseDTO> listMine(UUID userId) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), BusinessTime.ZONE);
        List<Trip> trips = tripRepository.findMine(userId, TripStatus.CLOSED, today.minusDays(CLOSED_TRIPS_DAYS));
        if (trips.isEmpty()) {
            return List.of();
        }
        Map<UUID, Reservation> mine = findMyReservations(userId, trips);
        return trips.stream()
                .map(trip -> viewMapper.trip(trip, userId, mine.get(trip.getId())))
                .toList();
    }

    /** The rules of §5.4 that need more than the body. Returns the campus of the request. */
    private Campus validate(User driver, PublishTripRequestDTO request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!driver.hasVehicle()) {
            errors.put("vehicle", "Para publicar un viaje tenés que cargar tu vehículo.");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), BusinessTime.ZONE);
        if (!LocalDateTime.of(request.departureDate(), request.departureTime()).isAfter(now)) {
            errors.put("departureDate", "Debe ser una fecha futura.");
        }
        Campus campus = campusRepository.findById(request.campus()).orElse(null);
        if (campus == null) {
            errors.put("campus", "La sede no existe.");
        } else if (!campus.getMeetingPoints().contains(request.meetingPoint())) {
            errors.put("meetingPoint", "No es un punto de encuentro de la sede.");
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return campus;
    }

    private Trip lockOwnTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!trip.isDrivenBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return trip;
    }

    /** The reservation of the user on each trip of the list, in one query. */
    private Map<UUID, Reservation> findMyReservations(UUID userId, List<Trip> trips) {
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();
        return reservationRepository.findByPassengerIdAndTripIdIn(userId, tripIds).stream()
                .collect(Collectors.toMap(reservation -> reservation.getTrip().getId(), Function.identity(),
                        BinaryOperator.maxBy(MOST_RELEVANT)));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    /** {@code created} decides between 201 (new) and 200 (retry of the same request). */
    public record PublishResult(TripResponseDTO trip, boolean created) {
    }
}
