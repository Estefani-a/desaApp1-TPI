package com.example.uniride.backend;

import com.example.uniride.backend.campus.CampusRepository;
import com.example.uniride.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** data.sql runs against the schema of the entities, and can run again without duplicating. */
class DataSqlIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DataSource dataSource;

    @Test
    void cargaLasSedesConSusPuntosYLosUsuariosDePrueba() {
        assertThat(campusRepository.findAll()).extracting("code")
                .containsExactlyInAnyOrder("MONSERRAT", "BELGRANO", "RECOLETA");
        assertThat(campusRepository.findById("MONSERRAT").orElseThrow().getMeetingPoints())
                .containsExactly("Salida Lima 717", "Salida Lima 757", "Salida Chile 1142", "Salida Independencia 1072");
        assertThat(userRepository.findByEmailIgnoreCase("conductora.demo@uade.edu.ar").orElseThrow().isActive())
                .isTrue();
    }

    @Test
    void ejecutarDataSqlOtraVezNoDuplicaNada() {
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);

        assertThat(campusRepository.count()).isEqualTo(3);
        assertThat(userRepository.count()).isEqualTo(3);
    }
}
