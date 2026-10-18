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

class ColetaResiliencePredicatesTest {

    private final ColetaRetryPredicate retryPredicate = new ColetaRetryPredicate();
    private final ColetaCircuitBreakerPredicate circuitBreakerPredicate = new ColetaCircuitBreakerPredicate();

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503})
    void respostaComErroDoServidor_retentaEContaParaOCircuito(int status) {
        ColetaApiException falha = new FalhaDeTeste(respostaHttp(status));

        assertThat(retryPredicate.test(falha)).isTrue();
        assertThat(circuitBreakerPredicate.test(falha)).isTrue();
    }

    @Test
    void falhaDeIo_retentaEContaParaOCircuito() {
        ColetaApiException falha = new FalhaDeTeste(new ResourceAccessException("I/O error", new IOException("connection reset")));

        assertThat(retryPredicate.test(falha)).isTrue();
        assertThat(circuitBreakerPredicate.test(falha)).isTrue();
    }

    @Test
    void rateLimitExcedido_naoRetentaMasContaParaOCircuito() {
        ColetaApiException falha = new FalhaDeTeste(respostaHttp(429));

        assertThat(retryPredicate.test(falha)).isFalse();
        assertThat(circuitBreakerPredicate.test(falha)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404})
    void respostaDefinitivaDoCliente_naoRetentaNemContaParaOCircuito(int status) {
        ColetaApiException falha = new FalhaDeTeste(respostaHttp(status));

        assertThat(retryPredicate.test(falha)).isFalse();
        assertThat(circuitBreakerPredicate.test(falha)).isFalse();
    }

    @Test
    void corpoInvalido_naoRetentaNemContaParaOCircuito() {
        ColetaApiException falha = new FalhaDeTeste(new RestClientException("conversao falhou"));

        assertThat(retryPredicate.test(falha)).isFalse();
        assertThat(circuitBreakerPredicate.test(falha)).isFalse();
    }

    @Test
    void falhaQueNaoEhDeColeta_naoRetentaNemContaParaOCircuito() {
        RuntimeException falha = new RuntimeException("falha qualquer");

        assertThat(retryPredicate.test(falha)).isFalse();
        assertThat(circuitBreakerPredicate.test(falha)).isFalse();
    }

    private static RestClientResponseException respostaHttp(int status) {
        return new RestClientResponseException("resposta " + status, HttpStatusCode.valueOf(status), "", null, null, null);
    }

    private static final class FalhaDeTeste extends ColetaApiException {

        private FalhaDeTeste(RestClientException causa) {
            super("Fonte de Teste", "detalhe da falha", causa);
        }
    }
}
