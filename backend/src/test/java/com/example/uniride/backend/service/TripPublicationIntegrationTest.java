package com.example.uniride.backend.service;

import com.example.uniride.backend.PostgresIntegrationTest;
import com.example.uniride.backend.TestData;
import com.example.uniride.backend.dto.PublishTripRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.model.Direction;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Publishing and "Mis viajes" (RF02) against the real PostgreSQL: the lock of the driver, the
 * query of §5.5 and the cancellation in one transaction. It removes what it creates.
 */
class TripPublicationIntegrationTest extends PostgresIntegrationTest {

    private static final UUID LUCIA = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");
    private static final UUID MARTIN = UUID.fromString("9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58");
    private static final UUID SOFIA = UUID.fromString("3b8d1f6a-9c2e-4d7b-8e1a-5f0c3a6d9b24");

    @Autowired
    private TripPublicationService service;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CampusRepository campusRepository;

    private final List<UUID> createdTrips = new ArrayList<>();

    @AfterEach
    void removeCreatedTrips() {
        for (UUID tripId : createdTrips) {
            reservationRepository.deleteAll(reservationRepository.findByTripIdOrderByReservedAtAsc(tripId));
            tripRepository.findById(tripId).ifPresent(tripRepository::delete);
        }
    }

    private static LocalDate today() {
        return LocalDate.now(BusinessTime.ZONE);
    }

    private UUID newTripId() {
        UUID id = UUID.randomUUID();
        createdTrips.add(id);
        return id;
    }

    private static PublishTripRequestDTO request() {
        return new PublishTripRequestDTO(Direction.VUELTA, "MONSERRAT", "Salida Lima 717", "Ramos Mejía", null,
                today().plusDays(5), LocalTime.of(22, 15), null, 3, 3500);
    }

    /** Saved directly: a published trip cannot leave in the past. */
    private UUID save(UUID driverId, LocalDate date, TripStatus status) {
        User driver = userRepository.findById(driverId).orElseThrow();
        Trip trip = new Trip(newTripId(), driver, campusRepository.findById("MONSERRAT").orElseThrow(),
                new TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", null),
                new TripSchedule(date, LocalTime.of(22, 15), null), 3, 3500, Instant.now());
        TestData.close(trip, status);
        return tripRepository.save(trip).getId();
    }

    private List<UUID> myTripIds(UUID userId) {
        return service.listMine(userId).stream().map(TripResponseDTO::tripId).toList();
    }

    @Test
    void reintentosSimultaneosDeLaMismaPublicacionCreanUnSoloViaje() throws Exception {
        UUID tripId = newTripId();
        int attempts = 4;
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return service.publish(LUCIA, tripId, request()).created();
                }));
            }
            start.countDown();

            List<Boolean> created = new ArrayList<>();
            for (Future<Boolean> result : results) {
                // get() rethrows: a retry that failed with ID_CONFLICT would break the test
                created.add(result.get(30, TimeUnit.SECONDS));
            }

            assertThat(created).containsExactlyInAnyOrder(true, false, false, false);
        } finally {
            executor.shutdownNow();
        }
        assertThat(myTripIds(LUCIA).stream().filter(tripId::equals)).hasSize(1);
    }

    @Test
    void misViajesTraeLosCerradosHastaSieteDiasAtras() {
        UUID sixDays = save(LUCIA, today().minusDays(6), TripStatus.FINALIZADO);
        UUID sevenDays = save(LUCIA, today().minusDays(7), TripStatus.FINALIZADO);
        UUID eightDays = save(LUCIA, today().minusDays(8), TripStatus.FINALIZADO);
        UUID oldCancelled = save(LUCIA, today().minusDays(8), TripStatus.CANCELADO);
        UUID futureCancelled = save(LUCIA, today().plusDays(3), TripStatus.CANCELADO);
        UUID oldOpen = save(LUCIA, today().minusDays(30), TripStatus.ABIERTO);

        List<UUID> mine = myTripIds(LUCIA);

        assertThat(mine).contains(sixDays, sevenDays, futureCancelled, oldOpen);
        assertThat(mine).doesNotContain(eightDays, oldCancelled);
    }

    @Test
    void misViajesTraeLosPublicadosYLosReservadosEnCualquierEstado() {
        UUID published = save(LUCIA, today().plusDays(5), TripStatus.ABIERTO);
        UUID reserved = save(SOFIA, today().plusDays(4), TripStatus.ABIERTO);
        UUID unrelated = save(SOFIA, today().plusDays(6), TripStatus.ABIERTO);
        UUID reservationId = UUID.randomUUID();
        reservationService.requestSeat(MARTIN, reserved, reservationId);
        reservationService.reject(SOFIA, reserved, reservationId);

        List<TripResponseDTO> martin = service.listMine(MARTIN);

        assertThat(martin).extracting(TripResponseDTO::tripId).contains(reserved).doesNotContain(published, unrelated);
        TripResponseDTO trip = martin.stream().filter(it -> it.tripId().equals(reserved)).findFirst().orElseThrow();
        assertThat(trip.myReservation().status()).isEqualTo(ReservationStatus.RECHAZADA);
        assertThat(trip.driverName()).isEqualTo("Sofía R.");
        assertThat(myTripIds(LUCIA)).contains(published).doesNotContain(reserved);
    }

    @Test
    void cancelarElViajeGuardaLasReservasCanceladas() {
        UUID tripId = newTripId();
        service.publish(LUCIA, tripId, request());
        UUID accepted = UUID.randomUUID();
        UUID pending = UUID.randomUUID();
        reservationService.requestSeat(MARTIN, tripId, accepted);
        reservationService.requestSeat(SOFIA, tripId, pending);
        reservationService.accept(LUCIA, tripId, accepted);

        service.cancel(LUCIA, tripId);

        assertThat(tripRepository.findById(tripId).orElseThrow().getStatus()).isEqualTo(TripStatus.CANCELADO);
        assertThat(reservationRepository.findByTripIdOrderByReservedAtAsc(tripId))
                .extracting(reservation -> reservation.getStatus())
                .containsExactly(ReservationStatus.CANCELADA, ReservationStatus.CANCELADA);
    }
}
