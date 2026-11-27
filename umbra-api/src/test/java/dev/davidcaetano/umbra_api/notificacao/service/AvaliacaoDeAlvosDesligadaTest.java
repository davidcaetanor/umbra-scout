package dev.davidcaetano.umbra_api.notificacao.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "umbra.notificacao.avaliacao-habilitada=false")
@ActiveProfiles("test")
class AvaliacaoDeAlvosDesligadaTest extends IntegrationTestBase {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void naoDeveCriarAAvaliacaoComOInterruptorDesligado() {
        assertThat(contexto.getBeanNamesForType(AvaliacaoDeAlvosService.class)).isEmpty();
    }
}
