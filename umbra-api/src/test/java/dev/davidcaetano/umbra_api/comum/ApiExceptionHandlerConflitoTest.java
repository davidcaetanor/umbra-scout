package dev.davidcaetano.umbra_api.comum;

import dev.davidcaetano.umbra_api.usuario.EmailJaCadastradoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerConflitoTest {

    @Test
    void deveResponder409EmProblemDetailSemEcoarOEmail() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/cadastro");

        ProblemDetail problema = new ApiExceptionHandler()
                .tratarRecursoEmConflito(new EmailJaCadastradoException(), request);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getType()).isEqualTo(URI.create("https://umbra.dev/erros/recurso-em-conflito"));
        assertThat(problema.getTitle()).isEqualTo("Recurso em conflito");
        assertThat(problema.getDetail()).isEqualTo("Já existe uma conta com este email.");
        assertThat(problema.getInstance()).isEqualTo(URI.create("/auth/cadastro"));
    }
}
