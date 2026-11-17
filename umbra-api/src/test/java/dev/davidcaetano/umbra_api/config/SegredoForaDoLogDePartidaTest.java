package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.coleta.itad.properties.ItadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class SegredoForaDoLogDePartidaTest {

    private static final String SEGREDO_CURTO = "segredo-curto-que-nao-pode-vazar";

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(UmbraProperties.class)
    static class SoPropriedades {
    }

    @Test
    void segredoDeJwtRecusadoNaPartida_naoDeveAparecerNoLog(CapturedOutput saida) {
        String segredoRecusado = SEGREDO_CURTO.substring(0, 31);
        SpringApplication aplicacao = new SpringApplication(SoPropriedades.class);
        aplicacao.setWebApplicationType(WebApplicationType.NONE);

        assertThatThrownBy(() -> aplicacao.run(
                "--umbra.jwt.secret=" + segredoRecusado,
                "--umbra.coleta.contato=contato-de-teste@umbra.invalid"))
                .hasStackTraceContaining("umbra.jwt.secret");

        assertThat(saida.getAll())
                .contains("umbra.jwt.secret")
                .doesNotContain(segredoRecusado);
    }

    @Test
    void chaveDoItad_naoDeveAparecerNoToString() {
        ItadProperties.ItadApi itad = new ItadProperties.ItadApi("chave-do-itad-que-nao-pode-vazar", "https://api.exemplo");

        assertThat(itad.toString()).doesNotContain("chave-do-itad-que-nao-pode-vazar").contains("apiKey=***");
    }
}
