package com.example.uniride.backend;

import com.example.uniride.backend.repository.CampusRepository;
import com.example.uniride.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Without the "demo" profile (a real deployment) the demo users, whose password is public,
 * do not exist. Uses its own empty database so the other tests' data does not interfere.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
})
class WithoutDemoProfileIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void sinElPerfilDemoHaySedesPeroNoUsuariosDePrueba() {
        assertThat(campusRepository.count()).isEqualTo(3);
        assertThat(userRepository.findByEmailIgnoreCase("conductora.demo@uade.edu.ar")).isEmpty();
        assertThat(userRepository.count()).isZero();
    }
}
