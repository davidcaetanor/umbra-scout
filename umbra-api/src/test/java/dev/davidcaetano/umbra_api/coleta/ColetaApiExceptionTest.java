package dev.davidcaetano.umbra_api.coleta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ColetaApiExceptionTest {

    private static final String FONTE = "Fonte de Teste";
    private static final String DETALHE = "detalhe da falha";

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void respostaComErroDoServidor_deveSerTransitoriaComStatus(int status) {
        ColetaApiException ex = new FalhaDeTeste(respostaHttp(status));

        assertThat(ex.tipo()).isEqualTo(TipoFalhaColeta.RESPOSTA_HTTP);
        assertThat(ex.status()).hasValue(status);
        assertThat(ex.transitoria()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 429})
    void respostaComErroDoCliente_naoDeveSerTransitoria(int status) {
        ColetaApiException ex = new FalhaDeTeste(respostaHttp(status));

        assertThat(ex.tipo()).isEqualTo(TipoFalhaColeta.RESPOSTA_HTTP);
        assertThat(ex.status()).hasValue(status);
        assertThat(ex.transitoria()).isFalse();
    }

    @Test
    void falhaDeAcesso_deveSerFalhaDeIoTransitoriaSemStatus() {
        ColetaApiException ex = new FalhaDeTeste(falhaDeIo());

        assertThat(ex.tipo()).isEqualTo(TipoFalhaColeta.FALHA_DE_IO);
        assertThat(ex.status()).isEmpty();
        assertThat(ex.transitoria()).isTrue();
    }

    @Test
    void outraFalhaDoRestClient_deveSerCorpoInvalidoNaoTransitorioSemStatus() {
        ColetaApiException ex = new FalhaDeTeste(new RestClientException("conversao falhou"));

        assertThat(ex.tipo()).isEqualTo(TipoFalhaColeta.CORPO_INVALIDO);
        assertThat(ex.status()).isEmpty();
        assertThat(ex.transitoria()).isFalse();
    }

    @Test
    void rateLimitExcedido_deveSerVerdadeiroSoNoStatus429() {
        assertThat(new FalhaDeTeste(respostaHttp(429)).rateLimitExcedido()).isTrue();
        assertThat(new FalhaDeTeste(respostaHttp(500)).rateLimitExcedido()).isFalse();
        assertThat(new FalhaDeTeste(respostaHttp(404)).rateLimitExcedido()).isFalse();
        assertThat(new FalhaDeTeste(falhaDeIo()).rateLimitExcedido()).isFalse();
        assertThat(new FalhaDeTeste(new RestClientException("conversao falhou")).rateLimitExcedido()).isFalse();
    }

    @Test
    void mensagem_deRespostaHttp_deveTrazerFonteStatusEDetalhe() {
        assertThat(new FalhaDeTeste(respostaHttp(503)).getMessage())
                .isEqualTo("Fonte de Teste respondeu 503: detalhe da falha");
    }

    @Test
    void mensagem_deFalhaDeIo_deveTrazerFonteEDetalheSemStatus() {
        assertThat(new FalhaDeTeste(falhaDeIo()).getMessage())
                .isEqualTo("Fonte de Teste nao respondeu (falha de I/O): detalhe da falha");
    }

    @Test
    void mensagem_deCorpoInvalido_deveTrazerFonteEDetalheSemStatus() {
        assertThat(new FalhaDeTeste(new RestClientException("conversao falhou")).getMessage())
                .isEqualTo("Fonte de Teste devolveu corpo que nao pode ser convertido: detalhe da falha");
    }

    private static RestClientResponseException respostaHttp(int status) {
        return new RestClientResponseException("resposta " + status, HttpStatusCode.valueOf(status), "", null, null, null);
    }

    private static ResourceAccessException falhaDeIo() {
        return new ResourceAccessException("I/O error", new IOException("connection reset"));
    }

    private static final class FalhaDeTeste extends ColetaApiException {

        private FalhaDeTeste(RestClientException causa) {
            super(FONTE, DETALHE, causa);
        }
    }
}
