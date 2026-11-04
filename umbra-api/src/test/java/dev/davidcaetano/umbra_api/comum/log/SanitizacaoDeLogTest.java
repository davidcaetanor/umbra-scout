package dev.davidcaetano.umbra_api.comum.log;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizacaoDeLogTest {

    @Test
    void paraLogDeveRemoverCaracteresDeControle() {
        String sanitizado = SanitizacaoDeLog.paraLog("/api\r\nINFO forjada\u0000\u001b[31m fim");

        assertThat(sanitizado).isEqualTo("/apiINFO forjada[31mfim");
    }

    @Test
    void paraLogDeveLimitarComprimento() {
        String sanitizado = SanitizacaoDeLog.paraLog("/" + "a".repeat(10_000));

        assertThat(sanitizado).hasSize(SanitizacaoDeLog.TAMANHO_MAXIMO);
    }

    @Test
    void paraLogDevePreservarUriComum() {
        assertThat(SanitizacaoDeLog.paraLog("/api/ofertas?q=jogo%20bom"))
                .isEqualTo("/api/ofertas?q=jogo%20bom");
    }

    @Test
    void paraLogDeveDevolverVazioQuandoValorNulo() {
        assertThat(SanitizacaoDeLog.paraLog(null)).isEmpty();
    }
}
