package com.example.uniride.backend.service;

import com.example.uniride.backend.dto.TripFiltersRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.repository.TripSpecifications;
import com.example.uniride.backend.util.BusinessTime;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Trips (docs/API.md §5). RF02 adds publishing here. */
@Service
public class TripService {

    /** docs/API.md §5.1: the list without {@code updatedSince} brings at most 100 trips. */
    static final int MAX_TRIPS = 100;

    /** Full list: by departure. The id breaks ties, so the order is always the same. */
    static final Sort BY_DEPARTURE = Sort.by("schedule.departureDate", "schedule.departureTime", "id");

    /** Partial refresh: by change, oldest first. */
    static final Sort BY_CHANGE = Sort.by("updatedAt", "id");

    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final ViewMapper viewMapper;
    private final Clock clock;

    public TripService(TripRepository tripRepository, ReservationRepository reservationRepository,
                       ViewMapper viewMapper, Clock clock) {
        this.tripRepository = tripRepository;
        this.reservationRepository = reservationRepository;
        this.viewMapper = viewMapper;
        this.clock = clock;
    }

    /**
     * {@code GET /trips} (RF03, docs/API.md §5.1). The filters combine with AND.
     *
     * <ul>
     *   <li>Without {@code updatedSince}: the {@code ABIERTO} and {@code COMPLETO} trips that did
     *       not leave yet, by departure, at most {@value #MAX_TRIPS}.</li>
     *   <li>With {@code updatedSince}: every trip changed since then, whatever its status, so the
     *       app can remove the finished or cancelled ones from Room. Sorted by {@code updatedAt}
     *       and without a limit: the app moves its cursor after each answer, so a trip left out
     *       would never be asked for again.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<TripResponseDTO> list(UUID viewerId, TripFiltersRequestDTO filters) {
        boolean partial = filters.updatedSince() != null;
        List<Trip> trips = tripRepository.findTrips(toSpecification(filters),
                partial ? BY_CHANGE : BY_DEPARTURE,
                partial ? Limit.unlimited() : Limit.of(MAX_TRIPS));
        if (trips.isEmpty()) {
            return List.of();
        }
        Map<UUID, Reservation> mine = findMyReservations(viewerId, trips);
        return trips.stream()
                .map(trip -> viewMapper.trip(trip, viewerId, mine.get(trip.getId())))
                .toList();
    }

    /** {@code GET /trips/{tripId}}: plate and phone only for the driver or an accepted passenger. */
    @Transactional(readOnly = true)
    public TripResponseDTO getDetail(UUID viewerId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return viewMapper.trip(trip, viewerId,
                pickMine(reservationRepository.findByTripIdAndPassengerIdOrderByReservedAtDesc(tripId, viewerId)));
    }

    private Specification<Trip> toSpecification(TripFiltersRequestDTO filters) {
        List<Specification<Trip>> conditions = new ArrayList<>();
        if (filters.updatedSince() == null) {
            conditions.add(TripSpecifications.statusIn(TripStatus.LISTED));
            conditions.add(TripSpecifications.notDepartedAt(LocalDateTime.ofInstant(clock.instant(), BusinessTime.ZONE)));
        } else {
            conditions.add(TripSpecifications.updatedSince(filters.updatedSince().toInstant()));
        }
        if (filters.campus() != null && !filters.campus().isBlank()) {
            conditions.add(TripSpecifications.campus(filters.campus().trim().toUpperCase(Locale.ROOT)));
        }
        if (filters.direction() != null) {
            conditions.add(TripSpecifications.direction(filters.direction()));
        }
        if (filters.date() != null && filters.shift() != null) {
            conditions.add(TripSpecifications.departureDateAndShift(filters.date(), filters.shift()));
        } else if (filters.date() != null) {
            conditions.add(TripSpecifications.departureDate(filters.date()));
        } else if (filters.shift() != null) {
            conditions.add(TripSpecifications.shift(filters.shift()));
        }
        return Specification.allOf(conditions);
    }

    /** The reservation of the viewer on each trip of the list, in one query. */
    private Map<UUID, Reservation> findMyReservations(UUID viewerId, List<Trip> trips) {
        List<UUID> tripIds = trips.stream().map(Trip::getId).toList();
        return reservationRepository.findByPassengerIdAndTripIdIn(viewerId, tripIds).stream()
                .sorted(Comparator.comparing(Reservation::getReservedAt).reversed())
                .collect(Collectors.groupingBy(reservation -> reservation.getTrip().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> pickMine(entry.getValue())));
    }

    /**
     * Reservation of the viewer on a trip, from [newestFirst]: the active one if there is one,
     * otherwise the newest (for example, a rejected request). {@code null} if the viewer never asked.
     */
    private static Reservation pickMine(List<Reservation> newestFirst) {
        return newestFirst.stream()
                .filter(reservation -> reservation.getStatus().isActive())
                .findFirst()
                .orElse(newestFirst.isEmpty() ? null : newestFirst.getFirst());
    }
}
