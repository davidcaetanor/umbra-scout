package dev.davidcaetano.umbra_api.comum;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerSegurancaTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ControllerQueLancaExcecaoDeSeguranca())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void authenticationExceptionDentroDoController_deveResponder401EmProblemDetailEmVezDe500() throws Exception {
        mockMvc.perform(get("/nao-autenticado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/nao-autenticado"))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/nao-autenticado"));
    }

    @Test
    void accessDeniedExceptionDentroDoController_deveResponder403EmProblemDetailEmVezDe500() throws Exception {
        mockMvc.perform(get("/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://umbra.dev/erros/acesso-negado"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/acesso-negado"));
    }

    @RestController
    static class ControllerQueLancaExcecaoDeSeguranca {

        @GetMapping("/nao-autenticado")
        void naoAutenticado() {
            throw new BadCredentialsException("credencial recusada");
        }

        @GetMapping("/acesso-negado")
        void acessoNegado() {
            throw new AccessDeniedException("acesso recusado");
        }
    }
}
