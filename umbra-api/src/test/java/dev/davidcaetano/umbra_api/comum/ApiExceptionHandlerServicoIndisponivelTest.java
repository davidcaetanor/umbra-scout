package dev.davidcaetano.umbra_api.comum;

import dev.davidcaetano.umbra_api.comum.excecao.ServicoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerServicoIndisponivelTest {

    private static final class IndisponivelDeTeste extends ServicoIndisponivelException {

        private IndisponivelDeTeste() {
            super("Fonte fora do ar.", Duration.ofSeconds(90), null);
        }
    }

    @Test
    void deveResponder503ComRetryAfterEmSegundos() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/monitoramentos");

        ResponseEntity<ProblemDetail> resposta = new ApiExceptionHandler()
                .tratarServicoIndisponivel(new IndisponivelDeTeste(), request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("90");
        ProblemDetail problema = resposta.getBody();
        assertThat(problema).isNotNull();
        assertThat(problema.getType()).isEqualTo(URI.create("https://umbra.dev/erros/servico-indisponivel"));
        assertThat(problema.getTitle()).isEqualTo("Serviço indisponível");
        assertThat(problema.getDetail()).isEqualTo("Fonte fora do ar.");
        assertThat(problema.getInstance()).isEqualTo(URI.create("/monitoramentos"));
    }
}
