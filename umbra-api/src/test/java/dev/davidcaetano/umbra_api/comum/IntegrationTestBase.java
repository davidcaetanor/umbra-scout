package dev.davidcaetano.umbra_api.comum;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestPropertySource(properties = {
        "umbra.coleta.contato=contato-de-teste@umbra.invalid",
        "umbra.jwt.secret=segredo-de-teste-com-pelo-menos-32-bytes"
})
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    static {
        POSTGRES.start();
    }
}
