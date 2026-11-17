package dev.davidcaetano.umbra_api.comum;

import dev.davidcaetano.umbra_api.comum.excecao.RegraDeNegocioVioladaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerRegraDeNegocioTest {

    private static final class RegraDeTeste extends RegraDeNegocioVioladaException {

        private RegraDeTeste() {
            super("Produto gratuito não aceita preço alvo.");
        }
    }

    @Test
    void deveResponder422EmProblemDetail() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/monitoramentos");

        ProblemDetail problema = new ApiExceptionHandler().tratarRegraDeNegocioViolada(new RegraDeTeste(), request);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(problema.getType()).isEqualTo(URI.create("https://umbra.dev/erros/regra-de-negocio-violada"));
        assertThat(problema.getTitle()).isEqualTo("Regra de negócio violada");
        assertThat(problema.getDetail()).isEqualTo("Produto gratuito não aceita preço alvo.");
        assertThat(problema.getInstance()).isEqualTo(URI.create("/monitoramentos"));
    }
}
