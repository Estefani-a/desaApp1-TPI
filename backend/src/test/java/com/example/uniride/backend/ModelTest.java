package com.example.uniride.backend;

import com.example.uniride.backend.trip.Shift;
import com.example.uniride.backend.user.AccountStatus;
import com.example.uniride.backend.user.PreferredMode;
import com.example.uniride.backend.user.User;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {

    private User user(String fullName) {
        return new User("demo@uade.edu.ar", "hash", fullName, "Ingeniería", "+5491100000000",
                PreferredMode.AMBOS, null);
    }

    @Test
    void cuentaNuevaQuedaPendienteHastaActivarla() {
        User user = user("Lucía Fernández");

        assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDIENTE);
        assertThat(user.isActive()).isFalse();

        user.activate();

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void nombrePublicoEsNombreEInicialDelApellido() {
        assertThat(user("Lucía Fernández").getPublicName()).isEqualTo("Lucía F.");
        assertThat(user("Martín  Ezequiel Gómez").getPublicName()).isEqualTo("Martín G.");
        assertThat(user("Cher").getPublicName()).isEqualTo("Cher");
    }

    @Test
    void turnoSeCalculaConLasFranjasDeA0() {
        assertThat(Shift.of(LocalTime.of(0, 0))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(12, 59))).isEqualTo(Shift.MANANA);
        assertThat(Shift.of(LocalTime.of(13, 0))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(17, 59))).isEqualTo(Shift.TARDE);
        assertThat(Shift.of(LocalTime.of(18, 0))).isEqualTo(Shift.NOCHE);
        assertThat(Shift.of(LocalTime.of(23, 59))).isEqualTo(Shift.NOCHE);
    }
}
