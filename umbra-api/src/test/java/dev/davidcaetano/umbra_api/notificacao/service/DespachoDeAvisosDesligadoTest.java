package dev.davidcaetano.umbra_api.notificacao.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.notificacao.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DespachoDeAvisosDesligadoTest extends IntegrationTestBase {

    @Autowired
    private ApplicationContext contexto;

    @Test
    void naoDeveCriarODespachoNemOEnvioComAFlagNoPadrao() {
        assertThat(contexto.getBeanNamesForType(DespachoDeAvisos.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(EmailService.class)).isEmpty();
    }
}
