package com.example.uniride.backend.service;

import com.example.uniride.backend.PostgresIntegrationTest;
import com.example.uniride.backend.TestData;
import com.example.uniride.backend.dto.TripFiltersRequestDTO;
import com.example.uniride.backend.dto.TripResponseDTO;
import com.example.uniride.backend.model.Direction;
import com.example.uniride.backend.model.Reservation;
import com.example.uniride.backend.model.ReservationStatus;
import com.example.uniride.backend.model.Shift;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filters of {@code GET /trips} (RF03) against the real PostgreSQL. Each test uses its own
 * fixed departure dates, far in the future and ten days apart from the ones of the other tests,
 * so the trips of other tests and of data-demo.sql do not get in the way.
 */
class TripListIntegrationTest extends PostgresIntegrationTest {

    private static final UUID LUCIA = UUID.fromString("6f1c2a4e-1b0e-4a8e-9a55-2c1f3e7b9d10");
    private static final UUID MARTIN = UUID.fromString("9a2e5c7b-3d8f-4b1a-a6e0-1c4f7d9b2e58");

    @Autowired
    private TripService service;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    /** Day [slot] of this class: each test uses its own slot, so their dates never meet. */
    private static LocalDate day(int slot) {
        return LocalDate.now(BusinessTime.ZONE).plusDays(1000 + 10L * slot);
    }

    private Trip save(String campus, Direction direction, LocalDate date, LocalTime time, int seats, Instant at) {
        User lucia = userRepository.findById(LUCIA).orElseThrow();
        return tripRepository.save(new Trip(UUID.randomUUID(), lucia, campusRepository.findById(campus).orElseThrow(),
                new TripRoute(direction, "Salida Lima 717", "Ramos Mejía", null),
                new TripSchedule(date, time, null), seats, 3500, at));
    }

    private Trip save(LocalDate date, LocalTime time) {
        return save("MONSERRAT", Direction.VUELTA, date, time, 3, Instant.now());
    }

    private Trip saveClosed(LocalDate date, TripStatus status, Instant at) {
        Trip trip = new Trip(UUID.randomUUID(), userRepository.findById(LUCIA).orElseThrow(),
                campusRepository.findById("MONSERRAT").orElseThrow(),
                new TripRoute(Direction.VUELTA, "Salida Lima 717", "Ramos Mejía", null),
                new TripSchedule(date, LocalTime.of(20, 0), null), 3, 3500, at);
        TestData.close(trip, status);
        return tripRepository.save(trip);
    }

    private static TripFiltersRequestDTO onDate(LocalDate date) {
        return new TripFiltersRequestDTO(null, null, date, null, null);
    }

    private static OffsetDateTime at(Instant instant) {
        return OffsetDateTime.ofInstant(instant, BusinessTime.ZONE);
    }

    private static List<UUID> ids(List<TripResponseDTO> trips) {
        return trips.stream().map(TripResponseDTO::tripId).toList();
    }

    @Test
    void losFiltrosDeSedeSentidoYFechaSeCombinanConY() {
        LocalDate day = day(1);
        Trip match = save("MONSERRAT", Direction.VUELTA, day, LocalTime.of(20, 0), 3, Instant.now());
        Trip otherDirection = save("MONSERRAT", Direction.IDA, day, LocalTime.of(20, 0), 3, Instant.now());
        Trip otherCampus = save("BELGRANO", Direction.VUELTA, day, LocalTime.of(20, 0), 3, Instant.now());
        Trip otherDay = save("MONSERRAT", Direction.VUELTA, day.plusDays(1), LocalTime.of(20, 0), 3, Instant.now());

        List<TripResponseDTO> result = service.list(MARTIN,
                new TripFiltersRequestDTO("monserrat", Direction.VUELTA, day, null, null));

        assertThat(ids(result)).containsExactly(match.getId());
        assertThat(ids(result)).doesNotContain(otherDirection.getId(), otherCampus.getId(), otherDay.getId());
    }

    @Test
    void laNocheDeUnDiaIncluyeLaMadrugadaSiguienteYNoLaDelMismoDia() {
        LocalDate day = day(2);
        Trip night = save(day, LocalTime.of(22, 15));
        Trip sameDayEarlyMorning = save(day, LocalTime.of(1, 0));
        Trip nextEarlyMorning = save(day.plusDays(1), LocalTime.of(1, 0));
        Trip nextSix = save(day.plusDays(1), LocalTime.of(6, 0));
        Trip nextNight = save(day.plusDays(1), LocalTime.of(22, 15));

        List<TripResponseDTO> result = service.list(MARTIN, new TripFiltersRequestDTO(null, null, day, Shift.NOCHE, null));

        assertThat(ids(result)).containsExactly(night.getId(), nextEarlyMorning.getId());
        assertThat(ids(result)).doesNotContain(sameDayEarlyMorning.getId(), nextSix.getId(), nextNight.getId());
    }

    @Test
    void laNocheSinFechaIncluyeLasMadrugadas() {
        LocalDate day = day(3);
        Trip night = save(day, LocalTime.of(18, 0));
        Trip earlyMorning = save(day, LocalTime.of(5, 59));
        Trip afternoon = save(day, LocalTime.of(17, 59));

        List<TripResponseDTO> result = service.list(MARTIN, new TripFiltersRequestDTO(null, null, null, Shift.NOCHE, null));

        assertThat(ids(result)).contains(night.getId(), earlyMorning.getId()).doesNotContain(afternoon.getId());
    }

    @Test
    void elTurnoMananaVaDeLasSeisALasTrece() {
        LocalDate day = day(4);
        Trip six = save(day, LocalTime.of(6, 0));
        Trip beforeSix = save(day, LocalTime.of(5, 59));
        Trip one = save(day, LocalTime.of(13, 0));

        List<TripResponseDTO> result = service.list(MARTIN, new TripFiltersRequestDTO(null, null, day, Shift.MANANA, null));

        assertThat(ids(result)).containsExactly(six.getId());
        assertThat(ids(result)).doesNotContain(beforeSix.getId(), one.getId());
    }

    @Test
    void noSeListanLosViajesQueYaSalieron() {
        LocalDate yesterday = LocalDate.now(BusinessTime.ZONE).minusDays(1);
        Trip departed = save(yesterday, LocalTime.of(22, 15));

        assertThat(ids(service.list(MARTIN, onDate(yesterday)))).doesNotContain(departed.getId());
    }

    @Test
    void losViajesCompletosSeListanParaQueLaAppLosActualice() {
        LocalDate day = day(5);
        Trip full = save("MONSERRAT", Direction.VUELTA, day, LocalTime.of(20, 0), 1, Instant.now());
        full.takeSeat(Instant.now());
        tripRepository.save(full);

        assertThat(service.list(MARTIN, onDate(day))).singleElement().satisfies(trip -> {
            assertThat(trip.tripId()).isEqualTo(full.getId());
            assertThat(trip.status()).isEqualTo(TripStatus.COMPLETO);
            assertThat(trip.availableSeats()).isZero();
        });
    }

    @Test
    void sinUpdatedSinceNoVienenLosFinalizadosNiLosCancelados() {
        LocalDate day = day(6);
        Trip open = save(day, LocalTime.of(20, 0));
        saveClosed(day, TripStatus.FINALIZADO, Instant.now());
        saveClosed(day, TripStatus.CANCELADO, Instant.now());

        assertThat(ids(service.list(MARTIN, onDate(day)))).containsExactly(open.getId());
    }

    @Test
    void conUpdatedSinceVienenTambienLosFinalizadosYLosCancelados() {
        LocalDate day = day(7);
        Instant since = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Trip finished = saveClosed(day, TripStatus.FINALIZADO, since.plusSeconds(1));
        Trip cancelled = saveClosed(day, TripStatus.CANCELADO, since.plusSeconds(2));

        List<TripResponseDTO> result = service.list(MARTIN,
                new TripFiltersRequestDTO(null, null, day, null, at(since)));

        assertThat(ids(result)).containsExactly(finished.getId(), cancelled.getId());
        assertThat(result).extracting(TripResponseDTO::status)
                .containsExactly(TripStatus.FINALIZADO, TripStatus.CANCELADO);
    }

    @Test
    void conUpdatedSinceSoloVienenLosQueCambiaronOrdenadosPorCambio() {
        LocalDate day = day(8);
        Instant since = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Trip old = save("MONSERRAT", Direction.VUELTA, day, LocalTime.of(20, 0), 3, since.minus(1, ChronoUnit.HOURS));
        Trip changedLater = save("MONSERRAT", Direction.VUELTA, day, LocalTime.of(7, 0), 3, since.plusSeconds(2));
        Trip changedFirst = save("MONSERRAT", Direction.VUELTA, day, LocalTime.of(21, 0), 3, since.plusSeconds(1));

        List<TripResponseDTO> result = service.list(MARTIN,
                new TripFiltersRequestDTO(null, null, day, null, at(since)));

        assertThat(ids(result)).containsExactly(changedFirst.getId(), changedLater.getId());
        assertThat(ids(result)).doesNotContain(old.getId());
    }

    @Test
    void cadaViajeTraeLaReservaDeQuienConsultaSinPatenteHastaQueLaAcepten() {
        LocalDate day = day(9);
        Trip trip = save(day, LocalTime.of(22, 0));
        User martin = userRepository.findById(MARTIN).orElseThrow();
        reservationRepository.save(new Reservation(UUID.randomUUID(), trip, martin, Instant.now()));

        assertThat(service.list(MARTIN, onDate(day))).singleElement().satisfies(listed -> {
            assertThat(listed.myReservation().status()).isEqualTo(ReservationStatus.PENDIENTE);
            assertThat(listed.vehiclePlate()).isNull();
            assertThat(listed.driverPhone()).isNull();
        });
    }
}
