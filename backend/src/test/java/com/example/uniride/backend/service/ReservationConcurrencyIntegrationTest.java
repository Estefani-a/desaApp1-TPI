package com.example.uniride.backend.service;

import com.example.uniride.backend.exception.ApiException;
import com.example.uniride.backend.repository.CampusRepository;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.exception.ErrorCode;
import com.example.uniride.backend.repository.ReservationRepository;
import com.example.uniride.backend.model.Trip;
import com.example.uniride.backend.repository.TripRepository;
import com.example.uniride.backend.model.TripRoute;
import com.example.uniride.backend.model.TripSchedule;
import com.example.uniride.backend.model.TripStatus;
import com.example.uniride.backend.model.User;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The row lock against the real PostgreSQL: simultaneous requests on the same trip never
 * oversell and never collide. Uses the demo users of data.sql.
 */
class ReservationConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final UUID LUCIA = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");
    private static final UUID MARTIN = UUID.fromString("9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58");
    private static final UUID SOFIA = UUID.fromString("3b8d1f6a-9c2e-4d7b-8e1a-5f0c3a6d9b24");

    @Autowired
    private ReservationService service;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private Trip tripWithOneSeat() {
        User lucia = userRepository.findById(LUCIA).orElseThrow();
        return tripRepository.save(new Trip(UUID.randomUUID(), lucia, campusRepository.findById("MONSERRAT").orElseThrow(),
                new TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", null),
                new TripSchedule(LocalDate.now().plusDays(1), LocalTime.of(22, 15), null),
                1, 3500, Instant.now()));
    }

    /** Runs the tasks at the same time and returns the error code of each one, or null if it worked. */
    private List<ErrorCode> runTogether(List<Callable<Object>> tasks) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<ErrorCode>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        task.call();
                        return null;
                    } catch (ApiException ex) {
                        return ex.getCode();
                    }
                }));
            }
            start.countDown();
            List<ErrorCode> results = new ArrayList<>();
            for (Future<ErrorCode> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void dosAceptacionesSimultaneasConUnaVacanteNoSobrevenden() throws Exception {
        Trip trip = tripWithOneSeat();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        service.requestSeat(MARTIN, trip.getId(), first);
        service.requestSeat(SOFIA, trip.getId(), second);

        List<ErrorCode> results = runTogether(List.of(
                () -> service.accept(LUCIA, trip.getId(), first),
                () -> service.accept(LUCIA, trip.getId(), second)));

        assertThat(results).containsExactlyInAnyOrder(null, ErrorCode.NO_SEATS_AVAILABLE);
        Trip reloaded = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(reloaded.getAvailableSeats()).isZero();
        assertThat(reloaded.getStatus()).isEqualTo(TripStatus.COMPLETO);
    }

    @Test
    void reintentosSimultaneosDeLaMismaSolicitudCreanUnaSolaReserva() throws Exception {
        Trip trip = tripWithOneSeat();
        UUID reservationId = UUID.randomUUID();

        List<ErrorCode> results = runTogether(List.of(
                () -> service.requestSeat(MARTIN, trip.getId(), reservationId),
                () -> service.requestSeat(MARTIN, trip.getId(), reservationId),
                () -> service.requestSeat(MARTIN, trip.getId(), reservationId)));

        assertThat(results).containsOnlyNulls();
        assertThat(reservationRepository.findByTripIdOrderByReservedAtAsc(trip.getId())).hasSize(1);
    }

    @Test
    void dosSolicitudesSimultaneasDelMismoPasajeroDejanUnaSolaActiva() throws Exception {
        Trip trip = tripWithOneSeat();

        List<ErrorCode> results = runTogether(List.of(
                () -> service.requestSeat(MARTIN, trip.getId(), UUID.randomUUID()),
                () -> service.requestSeat(MARTIN, trip.getId(), UUID.randomUUID())));

        assertThat(results).containsExactlyInAnyOrder(null, ErrorCode.ALREADY_RESERVED);
    }
}
