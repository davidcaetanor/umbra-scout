package dev.davidcaetano.umbra_api.actuator;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReadinessBancoOffTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void deveRetornar503QuandoBancoCair() {
        ResponseEntity<String> responseAntesCair =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(responseAntesCair.getStatusCode()).isEqualTo(HttpStatus.OK);

        postgres.stop();

        ResponseEntity<String> responseDepoisCair =
                restTemplate.getForEntity("/actuator/health/readiness", String.class);

        assertThat(responseDepoisCair.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
