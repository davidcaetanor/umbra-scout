package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.catalogo.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoControllerIpDeOrigemTest {

    private static final String ENDERECO_REMOTO = "203.0.113.7";

    private final ProdutoService produtoService = mock(ProdutoService.class);

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProdutoController(produtoService))
            .setControllerAdvice(new CodigosDoCatalogoAdvice())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();

    @Test
    void xForwardedFor_naoInfluencia_doisPedidosDoMesmoEnderecoRemotoContamParaOMesmoIp() throws Exception {
        mockMvc.perform(get("/produtos").param("nome", "elden ring").param("incluirExterno", "true")
                        .header("X-Forwarded-For", "198.51.100.1")
                        .with(enderecoRemoto(ENDERECO_REMOTO)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/produtos").param("nome", "hollow knight").param("incluirExterno", "true")
                        .header("X-Forwarded-For", "192.0.2.2, 198.51.100.3")
                        .with(enderecoRemoto(ENDERECO_REMOTO)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> ipDeOrigem = ArgumentCaptor.forClass(String.class);
        verify(produtoService, times(2)).buscar(any(), any(), any(), anyBoolean(), any(), ipDeOrigem.capture());
        assertThat(ipDeOrigem.getAllValues()).containsExactly(ENDERECO_REMOTO, ENDERECO_REMOTO);
    }

    private static RequestPostProcessor enderecoRemoto(String endereco) {
        return requisicao -> {
            requisicao.setRemoteAddr(endereco);
            return requisicao;
        };
    }
}
