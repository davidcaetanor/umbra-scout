package dev.davidcaetano.umbra_api.actuator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "umbra.jwt.secret=segredo-de-teste-do-perfil-prod",
        "umbra.coleta.contato=contato-de-teste@umbra.invalid",
        "itad.itad-api.api-key=chave-fake-de-teste-nunca-usada-de-verdade",
        "UMBRA_DB_URL=jdbc:postgresql://localhost:1/nao-usada",
        "UMBRA_DB_USER=usuario-nao-usado",
        "UMBRA_DB_PASSWORD=senha-nao-usada"
})
class PerfilProdTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    Environment environment;

    @Test
    void deveSubirSomenteComPerfilProd() {
        assertThat(environment.getActiveProfiles()).containsExactly("prod");
    }

    @Test
    void deveExporHealth() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/actuator/env",
            "/actuator/configprops",
            "/actuator/heapdump",
            "/v3/api-docs",
            "/swagger-ui/index.html"
    })
    void naoDeveExporRotaForaDoHealth(String rota) {
        assertThat(restTemplate.getForEntity(rota, String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
