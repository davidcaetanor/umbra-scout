package dev.davidcaetano.umbra_api.config.log;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filtro = new RequestIdFilter();

    @Test
    void deveGerarRequestIdEColocaNoMdcDuranteChamada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturadoDuranteChamada = new AtomicReference<>();

        FilterChain chain = (req, res) -> capturadoDuranteChamada.set(MDC.get(RequestIdFilter.MDC_KEY));

        filtro.doFilter(request, response, chain);

        assertThat(capturadoDuranteChamada.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(capturadoDuranteChamada.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void deveGerarUuidValidoQuandoHeaderAusente() throws Exception {
        String gerado = executarComHeader(null, new MockHttpServletResponse());

        assertThat(UUID.fromString(gerado).toString()).isEqualTo(gerado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void deveGerarUuidValidoQuandoHeaderEmBranco(String recebido) throws Exception {
        String gerado = executarComHeader(recebido, new MockHttpServletResponse());

        assertThat(gerado).isNotEqualTo(recebido);
        assertThat(UUID.fromString(gerado).toString()).isEqualTo(gerado);
    }

    @Test
    void deveReaproveitarUuidValidoRecebidoNoMdcENaResposta() throws Exception {
        String recebido = "3f2c1a7e-9b4d-4c8a-a1e2-5d6f7a8b9c0d";
        MockHttpServletResponse response = new MockHttpServletResponse();

        String capturado = executarComHeader(recebido, response);

        assertThat(capturado).isEqualTo(recebido);
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(recebido);
    }

    @Test
    void deveIgnorarValorQueNaoEUuid() throws Exception {
        assertIgnoradoEGerado("abc");
    }

    @Test
    void deveIgnorarValorComQuebraDeLinha() throws Exception {
        assertIgnoradoEGerado("aaa\nINFO linha forjada");
    }

    @Test
    void deveIgnorarValorDeDezMilCaracteres() throws Exception {
        assertIgnoradoEGerado("a".repeat(10_000));
    }

    @Test
    void sanitizarParaLogDeveRemoverCaracteresDeControle() {
        String sanitizado = RequestIdFilter.sanitizarParaLog("/api\r\nINFO forjada\u0000\u001b[31m\u2028fim");

        assertThat(sanitizado).isEqualTo("/apiINFO forjada[31mfim");
    }

    @Test
    void sanitizarParaLogDeveLimitarComprimento() {
        String sanitizado = RequestIdFilter.sanitizarParaLog("/" + "a".repeat(10_000));

        assertThat(sanitizado).hasSize(RequestIdFilter.TAMANHO_MAXIMO_LOG);
    }

    @Test
    void sanitizarParaLogDevePreservarUriComum() {
        assertThat(RequestIdFilter.sanitizarParaLog("/api/ofertas?q=jogo%20bom"))
                .isEqualTo("/api/ofertas?q=jogo%20bom");
    }

    private void assertIgnoradoEGerado(String recebido) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String capturado = executarComHeader(recebido, response);

        assertThat(capturado).isNotEqualTo(recebido);
        assertThat(UUID.fromString(capturado).toString()).isEqualTo(capturado);
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(capturado);
    }

    private String executarComHeader(String recebido, MockHttpServletResponse response) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (recebido != null) {
            request.addHeader(RequestIdFilter.HEADER, recebido);
        }
        AtomicReference<String> capturado = new AtomicReference<>();

        FilterChain chain = (req, res) -> capturado.set(MDC.get(RequestIdFilter.MDC_KEY));

        filtro.doFilter(request, response, chain);

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        return capturado.get();
    }
}
