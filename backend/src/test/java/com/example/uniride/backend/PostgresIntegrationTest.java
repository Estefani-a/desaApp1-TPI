package com.example.uniride.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base of the tests that need the real PostgreSQL: the schema that Hibernate creates,
 * {@code data.sql} and the row locks. Without Docker these tests are skipped, so
 * {@code ./mvnw verify} still works.
 *
 * <p>One container for the whole run, started by hand: Spring reuses the same context in
 * every subclass, so the container cannot stop after the first test class (Testcontainers'
 * singleton container pattern).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-prueba-de-al-menos-32-caracteres!!",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
})
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
