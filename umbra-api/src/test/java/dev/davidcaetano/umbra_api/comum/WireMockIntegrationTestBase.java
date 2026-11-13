package dev.davidcaetano.umbra_api.comum;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SpringBootTest
@ActiveProfiles("test")
public abstract class WireMockIntegrationTestBase extends IntegrationTestBase {

    protected static final WireMockServer WIREMOCK = WireMockServidorUnico.servidor();

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void apontarFontesParaOWireMock(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", WIREMOCK::baseUrl);
        registry.add("steam.steam-api.base-url", WIREMOCK::baseUrl);
        registry.add("kabum.kabum-api.base-url", WIREMOCK::baseUrl);
        registry.add("viacep.base-url", WIREMOCK::baseUrl);
    }

    @BeforeEach
    void zerarEstadoCompartilhadoEntreClasses() {
        WIREMOCK.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    protected static String fixture(String caminhoNoClasspath) {
        try (InputStream entrada = Objects.requireNonNull(
                WireMockIntegrationTestBase.class.getClassLoader().getResourceAsStream(caminhoNoClasspath),
                caminhoNoClasspath)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
